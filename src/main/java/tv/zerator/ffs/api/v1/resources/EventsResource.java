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
			throw new BadEntityException("STATUS_VALUE_INVALID");
		}
		
		try {
			if (startStr != null) start = Integer.parseInt(startStr);
		} catch (Exception e) {
			throw new BadEntityException("START_VALUE_INVALID");
		}
		
		try {
			if (endStr != null) end = Integer.parseInt(endStr);
		} catch (Exception e) {
			throw new BadEntityException("END_VALUE_INVALID");
		}
		
		if (end > 50) end = 50;
		
		return statusStr == null ? mEvents.getEvents(start, end) : mEvents.getEvents(status, start, end);
	}
	
	@Post
	public CreatedBean createEvent(EventCreateEntity entity) throws SQLException {
		ValidationUtils.verifyGroup(getRequest(), ApiV1.ADMIN);
		ValidationErrors err = new ValidationErrors();
		
		if (entity == null) throw new BadEntityException("ENTITY_NOT_FOUND");
		
		err.verifyFieldEmptyness("name", entity.name, 3, 200);
		err.verifyFieldEmptyness("description", entity.description, 3, 2048);
		
		err.checkErrors("CREATE_EVENT_ERROR");
		
		EventBean data = new EventBean();
		data.setDescription(entity.description);
		data.setName(entity.name);
		data.setReservedToAffiliates(entity.reserved_to_affiliates);
		data.setReservedToPartners(entity.reserved_to_partners);
		data.setStatus(entity.status);
		data.setMinimumViews(entity.minimum_views);
		data.setMinimumFollowers(entity.minimum_followers);
		data.setRankingType(entity.ranking_type);
		
		return new CreatedBean(mEvents.insert(data));
	}
	
	private static class EventCreateEntity {
		public String name, description;
		public boolean reserved_to_affiliates, reserved_to_partners;
		public int minimum_views, minimum_followers;
		public EventBean.Status status;
		public EventBean.RankingType ranking_type;
	}
}
