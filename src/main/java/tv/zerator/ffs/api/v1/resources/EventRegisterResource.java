package tv.zerator.ffs.api.v1.resources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
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
		if (event == null) throw new NotFoundException("Event not found.");
		
		if (event.getStatus() != EventBean.Status.OPEN) throw new ConflictException("The event is not opened.");

		AccountBean acc = (AccountBean) getRequest().getAttributes().get("account");
		if (mEvents.getRegistered(mEventId, acc.getTwitchId()) != null) throw new NotFoundException("You are already registered to this event.");
		
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
				if (retCode != 200 || channelObject.error != null) throw new NotFoundException("Channel not found on twitch api.");
				
				acc.setBroadcasterType(channelObject.broadcaster_type == null || channelObject.broadcaster_type.length() == 0 ? BroadcasterType.none : BroadcasterType.valueOf(channelObject.broadcaster_type));
				acc.setFollowers(channelObject.followers);
				acc.setTwitchId(channelObject._id);
				acc.setUrl(channelObject.url);
				acc.setUsername(channelObject.display_name);
				acc.setViews(channelObject.views);
				acc.setLogo(channelObject.logo);

				mAccounts.update(acc);

				mEvents.registerUser(mEventId, acc.getTwitchId(), UserStatus.AWAITING_FOR_ADMIN_VALIDATION, UUID.randomUUID().toString());
			} catch (URISyntaxException | IOException e) {
				Main.LOGGER.log(Level.SEVERE, "Impossible to login", e);
				throw new InternalServerError("Cannot verify your authentication. Try contacting an admin.");
			}
			return Status.SUCCESS_OK;
		} else throw new ConflictException("You are not eligible for this event.");
	}
	
	@Put
	public Status updatePariticipationStatus(EmailValidationBean bean) throws SQLException {
		if (bean == null) throw new BadEntityException("Entity not found.");
		ValidationErrors err = new ValidationErrors();
		
		err.verifyFieldEmptyness("key", bean.key, 5, 36);
		
		err.checkErrors("Cannot validate key format.");

		AccountBean acc = (AccountBean) getRequest().getAttributes().get("account");
		UserStatus status = mEvents.getUserStatusFromEmailKey(mEventId, acc.getTwitchId(), bean.key);
		if (status == null) throw new NotFoundException("Not registered or bad key.");
		if (status != UserStatus.AWAITING_FOR_EMAIL_VALIDATION) throw new ConflictException("You are not validated for this event yet.");
		
		mEvents.updateUser(mEventId, acc.getTwitchId(), UserStatus.VALIDATED);
		
		return Status.SUCCESS_OK;
	}
	
	@Delete
	public void unregisterToEvent() throws SQLException {
		if (mEvents.getEvent(mEventId) == null) throw new NotFoundException("Event not found.");
		
		AccountBean acc = (AccountBean) getRequest().getAttributes().get("account");
		if (mEvents.getRegistered(mEventId, acc.getTwitchId()) == null) throw new NotFoundException("Not registered to this event.");
		
		mEvents.removeUser(mEventId, acc.getTwitchId());
	}
	
	private static class EmailValidationBean {
		public String key;
	}
}
