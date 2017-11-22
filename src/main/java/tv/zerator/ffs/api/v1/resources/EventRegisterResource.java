package tv.zerator.ffs.api.v1.resources;

import java.sql.SQLException;
import java.util.UUID;

import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import alexmog.apilib.api.validation.ValidationErrors;
import alexmog.apilib.exceptions.BadEntityException;
import alexmog.apilib.exceptions.ConflictException;
import alexmog.apilib.exceptions.NotFoundException;
import alexmog.apilib.managers.DaoManager.DaoInject;
import tv.zerator.ffs.api.dao.EventsDao;
import tv.zerator.ffs.api.dao.EventsDao.UserStatus;
import tv.zerator.ffs.api.dao.beans.AccountBean;
import tv.zerator.ffs.api.dao.beans.AccountBean.BroadcasterType;
import tv.zerator.ffs.api.dao.beans.EventBean;

public class EventRegisterResource extends ServerResource {
	@DaoInject
	private static EventsDao mEvents;

	private int mEventId;

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
			mEvents.registerUser(mEventId, acc.getTwitchId(), UserStatus.AWAITING_FOR_ADMIN_VALIDATION, UUID.randomUUID().toString());
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
