package tv.zerator.ffs.api.v1.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.mrbean.MrBeanModule;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.net.HttpHeaders;

import alexmog.apilib.api.validation.ValidationErrors;
import alexmog.apilib.exceptions.BadAuthenticationException;
import alexmog.apilib.exceptions.BadEntityException;
import alexmog.apilib.exceptions.ConflictException;
import alexmog.apilib.exceptions.InternalServerError;
import alexmog.apilib.exceptions.NotFoundException;
import alexmog.apilib.managers.DaoManager.DaoInject;
import alexmog.apilib.managers.ServicesManager.Service;
import alexmog.apilib.services.EmailsService;
import tv.zerator.ffs.api.Main;
import tv.zerator.ffs.api.dao.AccountsDao;
import tv.zerator.ffs.api.dao.TokensDao;
import tv.zerator.ffs.api.dao.beans.AccountBean;
import tv.zerator.ffs.api.dao.beans.AccountBean.BroadcasterType;
import tv.zerator.ffs.api.v1.ApiV1;
import tv.zerator.ffs.api.dao.beans.TokenBean;


public class LoginResource extends ServerResource {
	@DaoInject
	private static AccountsDao mAccounts;
	@DaoInject
	private static TokensDao mTokens;
	@Service
	private static EmailsService mEmailsService;
	private static String mValidationCodeEmailContent = null, mTwitchUserUrl, mTwitchClientId, mTwitchChannelUrl, mTwitchAccept;

	private static final ObjectMapper mMapper;
	private final CloseableHttpClient mHttpClient = HttpClients.createDefault();

	static {
		mMapper = new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY).enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL)
		        .setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL).setVisibility(JsonMethod.FIELD, Visibility.ANY);
	    mMapper.registerModule(new MrBeanModule());
	    mMapper.disableDefaultTyping();
	}
	
	public LoginResource() throws IOException {
		if (mValidationCodeEmailContent != null) return;
		mValidationCodeEmailContent = FileUtils.readFileToString(new File("./email_templates/activation_key/index.html"));
		mTwitchUserUrl = Main.getInstance().getConfig().getProperty("twitch.user_url");
		mTwitchClientId = Main.getInstance().getConfig().getProperty("twitch.client_id");
		mTwitchChannelUrl = Main.getInstance().getConfig().getProperty("twitch.channels_url");
		mTwitchAccept = Main.getInstance().getConfig().getProperty("twitch.accept_header");
	}
	
	@Get
	public Status validateEmail() throws SQLException {
		String code = getQuery().getValues("code");
		if (code == null) throw new BadEntityException("CODE_NOT_FOUND");
		AccountBean bean = mAccounts.getFromValidationCode(code);
		if (bean != null) {
			if (bean.getGrade() != 0) throw new ConflictException("CODE_ALREADY_VALIDATED");
			bean.setGrade(ApiV1.USER);
			mAccounts.update(bean);
		} else throw new NotFoundException("CODE_UNKNOWN");
		return Status.SUCCESS_OK;
	}
	
	@Post
	public LoginResult login(LoginBean entity) throws SQLException, ClientProtocolException {
		ValidationErrors err = new ValidationErrors();
		
		if (entity == null) throw new BadEntityException("Entity not found.");
		
		err.verifyFieldEmptyness("twitch_token", entity.twitch_token, 1);
		
		err.checkErrors("LOGIN_ERROR");
		
		String json = null;
		int retCode = 200;
		TwitchUserObject userObject = null;
		LoginResult ret = new LoginResult();
		try {
			HttpGet httpGet = new HttpGet(new URI(mTwitchUserUrl));
			
			httpGet.addHeader("Accept", mTwitchAccept);
			httpGet.addHeader("Client-ID", mTwitchClientId);
			httpGet.addHeader("Authorization", "OAuth " + entity.twitch_token);
			
			try (CloseableHttpResponse resp = mHttpClient.execute(httpGet, HttpClientContext.create());) {
				InputStream stream = resp.getEntity().getContent();
				json = IOUtils.toString(stream);
				retCode = resp.getStatusLine().getStatusCode();
			}
			
			httpGet.releaseConnection();
			
			userObject = mMapper.readValue(json, TwitchUserObject.class);
			if (retCode != 200 || userObject.error != null) throw new BadAuthenticationException("BAD_AUTHENTICATION");
			
			httpGet.setURI(new URI(mTwitchChannelUrl + userObject._id));

			try (CloseableHttpResponse resp = mHttpClient.execute(httpGet, HttpClientContext.create());) {
				InputStream stream = resp.getEntity().getContent();
				json = IOUtils.toString(stream);
				retCode = resp.getStatusLine().getStatusCode();
			}
			
			httpGet.releaseConnection();
			
			TwitchChannelObject channelObject = mMapper.readValue(json, TwitchChannelObject.class);
			if (retCode != 200 || channelObject.error != null) throw new BadAuthenticationException("CHANNEL_NOT_FOUND_ON_TWITCH_API");
			
			AccountBean acc = mAccounts.get(channelObject._id);
			if (acc == null) {
				acc = new AccountBean();
				acc.setEmailActivationKey(UUID.randomUUID().toString());
				acc.setGrade(ApiV1.USER);
				ret.new_account = true;
			}
			
			acc.setBroadcasterType(channelObject.broadcaster_type == null || channelObject.broadcaster_type.length() == 0 ? BroadcasterType.none : BroadcasterType.valueOf(channelObject.broadcaster_type));
			acc.setEmail(userObject.email);
			acc.setFollowers(channelObject.followers);
			acc.setTwitchId(channelObject._id);
			acc.setUrl(channelObject.url);
			acc.setUsername(channelObject.display_name);
			acc.setViews(channelObject.views);
			acc.setLogo(channelObject.logo);

			if (ret.new_account) mAccounts.insert(acc);
			else mAccounts.update(acc);
			
			if (ret.new_account) {
				try {
					MimeMessage message = mEmailsService.generateMime("Validez votre compte!",
							mValidationCodeEmailContent.replaceAll("\\{CODE\\}", acc.getEmailActivationKey()), "text/html", acc.getEmail());
					mEmailsService.sendEmail(message);
				} catch (MessagingException e) {
					Main.LOGGER.log(Level.SEVERE, "Cannot send email.", e);
				}
			}
			
			TokenBean tokenBean = new TokenBean();
			tokenBean.setAccountId(acc.getTwitchId());
			tokenBean.setLastUsedTimestamp(System.currentTimeMillis());
			tokenBean.setToken(UUID.randomUUID().toString());
			
			mTokens.insert(tokenBean);
			
			ret.access_token = tokenBean.getToken();
			ret.expires_in = 86400000;
		} catch (URISyntaxException | IOException e) {
			Main.LOGGER.log(Level.SEVERE, "Impossible to login", e);
			throw new InternalServerError("TWITCH_API_ERROR");
		}
		return ret;
	}

	@Delete
	public void revokeToken() throws SQLException {
		String accessToken = getRequest().getHeaders().getFirstValue(HttpHeaders.AUTHORIZATION);
		if (accessToken != null) mTokens.delete(accessToken);
	}
	
	private static class LoginBean {
		public String twitch_token;
	}
	
	@SuppressWarnings("unused")
	private static class LoginResult {
		public String access_token;
		public int expires_in;
		public boolean new_account;
	}
	
	@SuppressWarnings("serial")
	@JsonInclude(Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class TwitchUserObject implements Serializable {
		public String error, email;
		public int _id;
	}

	@SuppressWarnings("serial")
	@JsonInclude(Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class TwitchChannelObject implements Serializable {
		public String display_name, url, error, broadcaster_type, logo;
		public int _id, views, followers;
	}
}
