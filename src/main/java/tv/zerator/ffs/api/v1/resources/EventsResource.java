package tv.zerator.ffs.api.v1.resources;

import java.sql.SQLException;
import java.util.List;

import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import alexmog.apilib.api.validation.ValidationErrors;
import alexmog.apilib.exceptions.BadEntityException;
import alexmog.apilib.managers.DaoManager.DaoInject;
import tv.zerator.ffs.api.dao.EventsDao;
import tv.zerator.ffs.api.dao.beans.EventBean;
import tv.zerator.ffs.api.utils.CreatedBean;
import tv.zerator.ffs.api.utils.ValidationUtils;
import tv.zerator.ffs.api.v1.ApiV1;

public class EventsResource extends ServerResource {
	@DaoInject
	private static EventsDao mEvents;
	
	@Get
	public List<EventBean> getEvents() throws SQLException {
		String statusStr = getQuery().getFirstValue("status");
		String startStr = getQuery().getFirstValue("start");
		String endStr = getQuery().getFirstValue("end");
		
		EventBean.Status status = EventBean.Status.OPEN;
		int start = 0, end = 50;
		
		try {
			if (statusStr != null) status = EventBean.Status.valueOf(statusStr);
		} catch (Exception e) {
			throw new BadEntityException("'status' value invalid.");
		}
		
		try {
			if (startStr != null) start = Integer.parseInt(startStr);
		} catch (Exception e) {
			throw new BadEntityException("'start' must be numeric.");
		}
		
		try {
			if (endStr != null) end = Integer.parseInt(endStr);
		} catch (Exception e) {
			throw new BadEntityException("'end' must be numeric.");
		}
		
		if (end > 50) end = 50;
		
		return statusStr == null ? mEvents.getEvents(start, end) : mEvents.getEvents(status, start, end);
	}
	
	@Post
	public CreatedBean createEvent(EventCreateEntity entity) throws SQLException {
		ValidationUtils.verifyGroup(getRequest(), ApiV1.ADMIN);
		ValidationErrors err = new ValidationErrors();
		
		if (entity == null) throw new BadEntityException("Entity not found.");
		
		err.verifyFieldEmptyness("name", entity.name, 3, 200);
		err.verifyFieldEmptyness("description", entity.description, 3, 2048);
		
		err.checkErrors("Cannot create event");
		
		EventBean data = new EventBean();
		data.setDescription(entity.description);
		data.setName(entity.name);
		data.setReservedToAffiliates(entity.reserved_to_affiliates);
		data.setReservedToPartners(entity.reserved_to_partners);
		data.setStatus(entity.status);
		
		return new CreatedBean(mEvents.insert(data));
	}
	
	private static class EventCreateEntity {
		public String name, description;
		public boolean reserved_to_affiliates, reserved_to_partners;
		public EventBean.Status status;
	}
}
