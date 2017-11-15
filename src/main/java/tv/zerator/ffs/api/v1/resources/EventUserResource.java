package tv.zerator.ffs.api.v1.resources;

import java.sql.SQLException;

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
import lombok.Data;
import tv.zerator.ffs.api.dao.EventsDao;
import tv.zerator.ffs.api.dao.EventsDao.AccountStatusBean;
import tv.zerator.ffs.api.dao.beans.AccountBean;
import tv.zerator.ffs.api.dao.beans.AccountBean.BroadcasterType;
import tv.zerator.ffs.api.utils.ValidationUtils;
import tv.zerator.ffs.api.v1.ApiV1;

public class EventUserResource extends ServerResource {
	@DaoInject
	private static EventsDao mEvents;

	private int mEventId, mUserId;
	
	@Override
	protected void doInit() throws ResourceException {
		mEventId = Integer.parseInt(getAttribute("EVENT_ID"));
		mUserId = Integer.parseInt(getAttribute("USER_ID"));
	}
	
	@Get
	public UserRepresentation getUser() throws SQLException {
		AccountStatusBean bean = mEvents.getRegistered(mEventId, mUserId);
		if (bean == null) throw new NotFoundException("User not found on this event.");

		Object obj = getRequest().getAttributes().get("account");
		boolean isModerator = obj == null ? false : ((AccountBean) obj).getGrade() >= ApiV1.MODERATOR;
		
		return isModerator ? new UserRepresentation(bean.getTwitchId(), bean.getViews(), bean.getFollowers(), bean.getGrade(), bean.getUsername(), bean.getEmail(), bean.getUrl(), bean.getLogo(), bean.getBroadcasterType(), bean.status)
				: new UserRepresentation(bean.getTwitchId(), bean.getViews(), bean.getFollowers(), bean.getGrade(), bean.getUsername(), null, bean.getUrl(), bean.getLogo(), null, bean.status);
	}
	
	@Put
	public Status updateUser(UpdateUserEntity entity) throws SQLException {
		ValidationUtils.verifyGroup(getRequest(), ApiV1.MODERATOR);
		
		if (entity == null) throw new BadEntityException("Entity not found.");

		AccountStatusBean bean = mEvents.getRegistered(mEventId, mUserId);
		if (bean == null) throw new NotFoundException("User not found on this event.");
		
		mEvents.updateUser(mEventId, mUserId, entity.status);
		return Status.SUCCESS_OK;
	}
	
	@Delete
	public Status deleteUser() throws SQLException {
		ValidationUtils.verifyGroup(getRequest(), ApiV1.MODERATOR);

		AccountStatusBean bean = mEvents.getRegistered(mEventId, mUserId);
		if (bean == null) throw new NotFoundException("User not found on this event.");
		
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
