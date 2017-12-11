package tv.zerator.ffs.api.v1.resources;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.FileUtils;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import alexmog.apilib.exceptions.BadEntityException;
import alexmog.apilib.exceptions.NotFoundException;
import alexmog.apilib.managers.DaoManager.DaoInject;
import alexmog.apilib.managers.ServicesManager.Service;
import alexmog.apilib.services.EmailsService;
import lombok.Data;
import tv.zerator.ffs.api.Main;
import tv.zerator.ffs.api.dao.AccountsDao;
import tv.zerator.ffs.api.dao.EventsDao;
import tv.zerator.ffs.api.dao.EventsDao.AccountStatusBean;
import tv.zerator.ffs.api.dao.EventsDao.UserStatus;
import tv.zerator.ffs.api.dao.EventsDao.UserStatusBean;
import tv.zerator.ffs.api.dao.beans.AccountBean;
import tv.zerator.ffs.api.dao.beans.EventBean;
import tv.zerator.ffs.api.dao.beans.AccountBean.BroadcasterType;
import tv.zerator.ffs.api.utils.ValidationUtils;
import tv.zerator.ffs.api.v1.ApiV1;

public class EventUserResource extends ServerResource {
	@DaoInject
	private static EventsDao mEvents;
	@DaoInject
	private static AccountsDao mAccountsDao;
	@Service
	private static EmailsService mEmailsService;
	private static String mValidateEmail, mValidatedEmail;
	private int mEventId, mUserId;
	
	static {
		try {
			mValidateEmail = FileUtils.readFileToString(new File("./email_templates/validation_event_pariticipation/index.html"));
			mValidatedEmail = FileUtils.readFileToString(new File("./email_templates/validated_participant/index.html"));
		} catch (IOException e) {
			Main.LOGGER.log(Level.SEVERE, "Cannot init.", e);
			System.exit(1);
		}
	}
	
	@Override
	protected void doInit() throws ResourceException {
		mEventId = Integer.parseInt(getAttribute("EVENT_ID"));
		mUserId = Integer.parseInt(getAttribute("USER_ID"));
	}
	
	@Get
	public UserRepresentation getUser() throws SQLException {
		AccountStatusBean bean = mEvents.getRegistered(mEventId, mUserId);
		if (bean == null) throw new NotFoundException("USER_NOT_REGISTERED");

		Object obj = getRequest().getAttributes().get("account");
		boolean isModerator = obj == null ? false : ((AccountBean) obj).getGrade() >= ApiV1.MODERATOR;
		
		return isModerator ? new UserRepresentation(bean.getTwitchId(), bean.getViews(), bean.getFollowers(), bean.getGrade(), bean.getUsername(), bean.getEmail(), bean.getUrl(), bean.getLogo(), bean.getBroadcasterType(), bean.status)
				: new UserRepresentation(bean.getTwitchId(), bean.getViews(), bean.getFollowers(), bean.getGrade(), bean.getUsername(), null, bean.getUrl(), bean.getLogo(), null, bean.status);
	}
	
	@Put
	public Status updateUser(UpdateUserEntity entity) throws SQLException {
		ValidationUtils.verifyGroup(getRequest(), ApiV1.MODERATOR);
		
		if (entity == null) throw new BadEntityException("ENTITY_NOT_FOUND");

		AccountStatusBean bean = mEvents.getRegistered(mEventId, mUserId);
		if (bean == null) throw new NotFoundException("USER_NOT_REGISTERED");
		
		mEvents.updateUser(mEventId, mUserId, entity.status);
		try {
			if (entity.status == UserStatus.AWAITING_FOR_EMAIL_VALIDATION) {
				EventBean event = mEvents.getEvent(mEventId);
				UserStatusBean user = mEvents.getUser(mEventId, mUserId);
				MimeMessage message = mEmailsService.generateMime("FFS - Validation de votre participation a l'event " + event.getName(),
						mValidateEmail.replaceAll("\\{EVENT_NAME\\}", event.getName())
						.replaceAll("\\{EVENT_ID\\}", event.getId() + "")
						.replaceAll("\\{CODE\\}", user.getEmailActivationKey()), "text/html", user.getAccount().getEmail());
				mEmailsService.sendEmail(message);
			} else if (entity.status == UserStatus.VALIDATED) {
				EventBean event = mEvents.getEvent(mEventId);
				UserStatusBean user = mEvents.getUser(mEventId, mUserId);
				MimeMessage message = mEmailsService.generateMime("FFS - Votre participation a l'event " + event.getName() + " a ete validee",
						mValidatedEmail.replaceAll("\\{EVENT_NAME\\}", event.getName())
						.replaceAll("\\{EVENT_ID\\}", event.getId() + ""), "text/html", user.getAccount().getEmail());
				mEmailsService.sendEmail(message);
			}
		} catch (MessagingException e) {
			Main.LOGGER.log(Level.SEVERE, "Cannot send email.", e);
		}
		return Status.SUCCESS_OK;
	}
	
	@Delete
	public Status deleteUser() throws SQLException {
		ValidationUtils.verifyGroup(getRequest(), ApiV1.MODERATOR);

		AccountStatusBean bean = mEvents.getRegistered(mEventId, mUserId);
		if (bean == null) throw new NotFoundException("USER_NOT_REGISTERED");
		
		mEvents.removeUser(mEventId, mUserId);
		return Status.SUCCESS_OK;
	}
	
	@JsonInclude(Include.NON_NULL)
	private @Data static class UserRepresentation {
		private final int twitchId, views, followers, grade;
		private final String username, email, url, logo;
		private final BroadcasterType broadcasterType;
		private final EventsDao.UserStatus status;
	}
	
	private static class UpdateUserEntity {
		public EventsDao.UserStatus status;
	}
}
