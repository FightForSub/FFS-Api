package tv.zerator.ffs.api.v1.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.mrbean.MrBeanModule;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import alexmog.apilib.api.validation.ValidationErrors;
import alexmog.apilib.exceptions.BadEntityException;
import alexmog.apilib.exceptions.ConflictException;
import alexmog.apilib.exceptions.InternalServerError;
import alexmog.apilib.exceptions.NotFoundException;
import alexmog.apilib.managers.DaoManager.DaoInject;
import alexmog.apilib.managers.ServicesManager.Service;
import alexmog.apilib.services.EmailsService;
import tv.zerator.ffs.api.Main;
import tv.zerator.ffs.api.dao.AccountsDao;
import tv.zerator.ffs.api.dao.EventsDao;
import tv.zerator.ffs.api.dao.EventsDao.UserStatus;
import tv.zerator.ffs.api.dao.beans.AccountBean;
import tv.zerator.ffs.api.dao.beans.AccountBean.BroadcasterType;
import tv.zerator.ffs.api.v1.resources.LoginResource.TwitchChannelObject;
import tv.zerator.ffs.api.dao.beans.EventBean;

public class EventRegisterResource extends ServerResource {
	@DaoInject
	private static AccountsDao mAccounts;
	@DaoInject
	private static EventsDao mEvents;
	@Service
	private static EmailsService mEmailsService;
	private static final ObjectMapper mMapper;
	private final CloseableHttpClient mHttpClient = HttpClients.createDefault();
	private static String mTwitchUserUrl, mTwitchClientId, mTwitchChannelUrl = null, mTwitchAccept;
	private static String mValidatedEmail;

	private int mEventId;
	
	static {
		mMapper = new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY).enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL)
		        .setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL).setVisibility(JsonMethod.FIELD, Visibility.ANY);
	    mMapper.registerModule(new MrBeanModule());
	    mMapper.disableDefaultTyping();
	    try {
			mValidatedEmail = FileUtils.readFileToString(new File("./email_templates/validated_participant/index.html"));
		} catch (IOException e) {
			Main.LOGGER.log(Level.SEVERE, "Cannot init.", e);
			System.exit(1);
		}
	}
	
	public EventRegisterResource() {
		if (mTwitchChannelUrl != null) return;
		mTwitchUserUrl = Main.getInstance().getConfig().getProperty("twitch.user_url");
		mTwitchClientId = Main.getInstance().getConfig().getProperty("twitch.client_id");
		mTwitchChannelUrl = Main.getInstance().getConfig().getProperty("twitch.channels_url");
		mTwitchAccept = Main.getInstance().getConfig().getProperty("twitch.accept_header");
	}

	@Override
	protected void doInit() throws ResourceException {
		mEventId = Integer.parseInt(getAttribute("EVENT_ID"));
	}
	
	@Post
	public Status registerToEvent() throws SQLException {
		EventBean event = mEvents.getEvent(mEventId);
		if (event == null) throw new NotFoundException("EVENT_NOT_FOUND");
		
		if (event.getStatus() != EventBean.Status.OPEN) throw new ConflictException("EVENT_NOT_OPENED");

		AccountBean acc = (AccountBean) getRequest().getAttributes().get("account");
		if (mEvents.getRegistered(mEventId, acc.getTwitchId()) != null) throw new ConflictException("ALREADY_REGISTERED");
		
		if (!event.isReservedToAffiliates() && !event.isReservedToPartners()
				|| event.isReservedToAffiliates() && acc.getBroadcasterType() == BroadcasterType.affiliate
				|| event.isReservedToPartners() && acc.getBroadcasterType() == BroadcasterType.partner) {
			try {
				String json;
				int retCode;
				HttpGet httpGet;
				httpGet = new HttpGet(new URI(mTwitchUserUrl));
				httpGet.addHeader("Accept", mTwitchAccept);
				httpGet.addHeader("Client-ID", mTwitchClientId);
				httpGet.setURI(new URI(mTwitchChannelUrl + acc.getTwitchId()));

				try (CloseableHttpResponse resp = mHttpClient.execute(httpGet, HttpClientContext.create());) {
					InputStream stream = resp.getEntity().getContent();
					json = IOUtils.toString(stream);
					retCode = resp.getStatusLine().getStatusCode();
				}
				
				httpGet.releaseConnection();
				
				TwitchChannelObject channelObject = mMapper.readValue(json, TwitchChannelObject.class);
				if (retCode != 200 || channelObject.error != null) throw new NotFoundException("CHANNEL_NOT_FOUND_ON_TWITCH_API");
				
				acc.setBroadcasterType(channelObject.broadcaster_type == null || channelObject.broadcaster_type.length() == 0 ? BroadcasterType.none : BroadcasterType.valueOf(channelObject.broadcaster_type));
				acc.setFollowers(channelObject.followers);
				acc.setTwitchId(channelObject._id);
				acc.setUrl(channelObject.url);
				acc.setUsername(channelObject.display_name);
				acc.setViews(channelObject.views);
				acc.setLogo(channelObject.logo);

				mAccounts.update(acc);

				if (acc.getViews() < event.getMinimumViews()) throw new ConflictException("NEED_MORE_VIEWS");
				if (acc.getFollowers() < event.getMinimumFollowers()) throw new ConflictException("NEED_MORE_FOLLOWERS");
				
				mEvents.registerUser(mEventId, acc.getTwitchId(), UserStatus.AWAITING_FOR_ADMIN_VALIDATION, UUID.randomUUID().toString());
			} catch (URISyntaxException | IOException e) {
				Main.LOGGER.log(Level.SEVERE, "Impossible to login", e);
				throw new InternalServerError("TWITCH_API_ERROR");
			}
			return Status.SUCCESS_OK;
		} else throw new ConflictException("NOT_ELIGIBLE");
	}
	
	@Put
	public Status updatePariticipationStatus(EmailValidationBean bean) throws SQLException {
		if (bean == null) throw new BadEntityException("EVENT_NOT_FOUND");
		ValidationErrors err = new ValidationErrors();
		
		err.verifyFieldEmptyness("key", bean.key, 5, 36);
		
		err.checkErrors("INVALID_KEY");

		AccountBean acc = (AccountBean) getRequest().getAttributes().get("account");
		UserStatus status = mEvents.getUserStatusFromEmailKey(mEventId, acc.getTwitchId(), bean.key);
		if (status == null) throw new NotFoundException("UNKNOWN_KEY");
		if (status != UserStatus.AWAITING_FOR_EMAIL_VALIDATION) throw new ConflictException("BAD_STATUS");
		
		mEvents.updateUser(mEventId, acc.getTwitchId(), UserStatus.VALIDATED);
		try {
			EventBean event = mEvents.getEvent(mEventId);
			MimeMessage message = mEmailsService.generateMime("Validez votre compte!",
					mValidatedEmail.replaceAll("\\{EVENT_NAME\\}", event.getName())
					.replaceAll("\\{EVENT_ID\\}", event.getId() + ""), "text/html", acc.getEmail());
			mEmailsService.sendEmail(message);
		} catch (MessagingException e) {
			Main.LOGGER.log(Level.SEVERE, "Cannot send email.", e);
		}
		return Status.SUCCESS_OK;
	}
	
	@Delete
	public void unregisterToEvent() throws SQLException {
		EventBean event = mEvents.getEvent(mEventId);
		if (event == null) throw new NotFoundException("EVENT_NOT_FOUND");
		if (event.getStatus() != EventBean.Status.OPEN) throw new ConflictException("EVENT_NOT_OPENED");
		
		AccountBean acc = (AccountBean) getRequest().getAttributes().get("account");
		if (mEvents.getRegistered(mEventId, acc.getTwitchId()) == null) throw new NotFoundException("NOT_REGISTERED");
		
		mEvents.removeUser(mEventId, acc.getTwitchId());
	}
	
	private static class EmailValidationBean {
		public String key;
	}
}
