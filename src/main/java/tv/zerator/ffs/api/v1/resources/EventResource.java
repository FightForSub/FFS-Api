package tv.zerator.ffs.api.v1.resources;

import java.sql.SQLException;

import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import alexmog.apilib.api.validation.ValidationErrors;
import alexmog.apilib.exceptions.BadEntityException;
import alexmog.apilib.exceptions.NotFoundException;
import alexmog.apilib.managers.DaoManager.DaoInject;
import tv.zerator.ffs.api.dao.EventsDao;
import tv.zerator.ffs.api.dao.beans.EventBean;
import tv.zerator.ffs.api.utils.ValidationUtils;
import tv.zerator.ffs.api.v1.ApiV1;

public class EventResource extends ServerResource {
	@DaoInject
	private static EventsDao mEvents;
	
	private int mEventId;

	@Override
	protected void doInit() throws ResourceException {
		mEventId = Integer.parseInt(getAttribute("EVENT_ID"));
	}
	
	@Get
	public EventBean getEvent() throws SQLException {
		EventBean event = mEvents.getEvent(mEventId);
		if (event == null) throw new NotFoundException("Event not found.");
		return event;
	}
	
	@Put
	public Status updateEvent(UpdateEventEntity entity) throws SQLException {
		ValidationUtils.verifyGroup(getRequest(), ApiV1.ADMIN);
		ValidationErrors err = new ValidationErrors();
		
		if (entity == null) throw new BadEntityException("Entity not found.");
		
		err.verifyFieldEmptyness("name", entity.name, 3, 200);
		err.verifyFieldEmptyness("description", entity.description, 3, 2048);
		
		err.checkErrors("Cannot create event");
		
		EventBean bean = mEvents.getEvent(mEventId);
		if (bean == null) throw new NotFoundException("Event not found.");
		bean.setCurrent(entity.current);
		bean.setDescription(entity.description);
		bean.setName(entity.name);
		bean.setReservedToAffiliates(entity.reserved_to_affiliates);
		bean.setReservedToPartners(entity.reserved_to_partners);
		bean.setStatus(entity.status);
		mEvents.update(bean);
		return Status.SUCCESS_OK;
	}
	
	@Delete
	public Status deleteEvent() throws SQLException {
   		ValidationUtils.verifyGroup(getRequest(), ApiV1.ADMIN);
		mEvents.delete(mEventId);
		return Status.SUCCESS_OK;
	}
	
	private static class UpdateEventEntity {
		public String name, description;
		public boolean current, reserved_to_affiliates, reserved_to_partners;
		public EventBean.Status status;
	}
}
