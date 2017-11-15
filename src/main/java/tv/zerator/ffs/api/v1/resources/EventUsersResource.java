package tv.zerator.ffs.api.v1.resources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

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
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import alexmog.apilib.exceptions.BadEntityException;
import alexmog.apilib.exceptions.ConflictException;
import alexmog.apilib.exceptions.InternalServerError;
import alexmog.apilib.exceptions.NotFoundException;
import alexmog.apilib.managers.DaoManager.DaoInject;
import lombok.Data;
import tv.zerator.ffs.api.Main;
import tv.zerator.ffs.api.dao.AccountsDao;
import tv.zerator.ffs.api.dao.EventsDao;
import tv.zerator.ffs.api.dao.beans.AccountBean;
import tv.zerator.ffs.api.dao.beans.AccountBean.BroadcasterType;
import tv.zerator.ffs.api.utils.ValidationUtils;
import tv.zerator.ffs.api.v1.ApiV1;
import tv.zerator.ffs.api.v1.resources.LoginResource.TwitchChannelObject;

public class EventUsersResource extends ServerResource {
	@DaoInject
	private static AccountsDao mAccounts;
	@DaoInject
	private static EventsDao mEvents;
	private static final ObjectMapper mMapper;
	private final CloseableHttpClient mHttpClient = HttpClients.createDefault();
	private static String mTwitchUserUrl, mTwitchClientId, mTwitchChannelUrl = null, mTwitchAccept;

	private int mEventId;
	
	static {
		mMapper = new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY).enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL)
		        .setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL).setVisibility(JsonMethod.FIELD, Visibility.ANY);
	    mMapper.registerModule(new MrBeanModule());
	    mMapper.disableDefaultTyping();
	}
	
	public EventUsersResource() {
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
	
	@Get
	public List<UserRepresentation> getUsers() throws SQLException {
		String statusStr = getQuery().getFirstValue("status");
		EventsDao.UserStatus status = EventsDao.UserStatus.VALIDATED;
		try {
			if (statusStr != null) status = EventsDao.UserStatus.valueOf(statusStr);
		} catch (Exception e) {
			throw new BadEntityException("'status' bad format.");
		}
		
		if (mEvents.getEvent(mEventId) == null) throw new NotFoundException("Event not found.");
		
		Object obj = getRequest().getAttributes().get("account");
		boolean isModerator = obj == null ? false : ((AccountBean) obj).getGrade() >= ApiV1.MODERATOR;
		
		List<AccountBean> users = statusStr != null ? mEvents.getUsers(mEventId, status) : mEvents.getUsers(mEventId);
		List<UserRepresentation> ret = new ArrayList<>();
		
		for (AccountBean ac : users) ret.add(isModerator ? new UserRepresentation(ac.getTwitchId(), ac.getViews(), ac.getFollowers(), ac.getGrade(), ac.getUsername(), ac.getEmail(), ac.getUrl(), ac.getLogo(), ac.getBroadcasterType())
				: new UserRepresentation(ac.getTwitchId(), ac.getViews(), ac.getFollowers(), ac.getGrade(), ac.getUsername(), null, ac.getUrl(), ac.getLogo(), null));
		
		return ret;
	}
	
	@Post
	public Status registerUser(RegisterUserEntity entity) throws SQLException {
		ValidationUtils.verifyGroup(getRequest(), ApiV1.MODERATOR);
		if (entity == null) throw new BadEntityException("Entity not found.");
		if (entity.status == null) throw new BadEntityException("Status is null.");
		if (mEvents.getEvent(mEventId) == null) throw new NotFoundException("Event not found.");
		if (mEvents.getRegistered(mEventId, entity.twitch_id) != null) throw new ConflictException("User already registered on this event.");
		try {
			String json;
			int retCode;
			HttpGet httpGet;
			httpGet = new HttpGet(new URI(mTwitchUserUrl));
			httpGet.addHeader("Accept", mTwitchAccept);
			httpGet.addHeader("Client-ID", mTwitchClientId);
			httpGet.setURI(new URI(mTwitchChannelUrl + entity.twitch_id));

			try (CloseableHttpResponse resp = mHttpClient.execute(httpGet, HttpClientContext.create());) {
				InputStream stream = resp.getEntity().getContent();
				json = IOUtils.toString(stream);
				retCode = resp.getStatusLine().getStatusCode();
			}
			
			httpGet.releaseConnection();
			
			TwitchChannelObject channelObject = mMapper.readValue(json, TwitchChannelObject.class);
			if (retCode != 200 || channelObject.error != null) throw new NotFoundException("Channel not found on twitch api.");
			
			boolean accountNew = false;
			AccountBean acc = mAccounts.get(channelObject._id);
			if (acc == null) {
				acc = new AccountBean();
				acc.setEmailActivationKey(UUID.randomUUID().toString());
				acc.setGrade(ApiV1.USER);
				accountNew = true;
			}
			
			acc.setBroadcasterType(channelObject.broadcaster_type == null || channelObject.broadcaster_type.length() == 0 ? BroadcasterType.none : BroadcasterType.valueOf(channelObject.broadcaster_type));
			acc.setFollowers(channelObject.followers);
			acc.setTwitchId(channelObject._id);
			acc.setEmail("");
			acc.setUrl(channelObject.url);
			acc.setUsername(channelObject.display_name);
			acc.setViews(channelObject.views);
			acc.setLogo(channelObject.logo);

			if (accountNew) mAccounts.insert(acc);
			else mAccounts.update(acc);
			
			mEvents.registerUser(mEventId, acc.getTwitchId(), entity.status, UUID.randomUUID().toString());
		} catch (URISyntaxException | IOException e) {
			Main.LOGGER.log(Level.SEVERE, "Impossible to login", e);
			throw new InternalServerError("Cannot verify your authentication. Try contacting an admin.");
		}
		
		return Status.SUCCESS_OK;
	}
	
	@JsonInclude(Include.NON_NULL)
	private @Data static class UserRepresentation {
		private final int twitchId, views, followers, grade;
		private final String username, email, url, logo;
		private final BroadcasterType broadcasterType;
	}
	
	private static class RegisterUserEntity {
		public int twitch_id;
		public EventsDao.UserStatus status;
	}
}
