package tv.zerator.ffs.api.v1.resources;

import java.sql.SQLException;
import java.util.List;

import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import alexmog.apilib.exceptions.NotFoundException;
import alexmog.apilib.managers.DaoManager.DaoInject;
import tv.zerator.ffs.api.dao.EventsDao;
import tv.zerator.ffs.api.utils.CreatedBean;
import tv.zerator.ffs.api.utils.ValidationUtils;
import tv.zerator.ffs.api.v1.ApiV1;

public class EventRoundsResource extends ServerResource {
	@DaoInject
	private static EventsDao mEvents;
	
	private int mEventId;

	@Override
	protected void doInit() throws ResourceException {
		mEventId = Integer.parseInt(getAttribute("EVENT_ID"));
	}
	
	@Get
	public List<Integer> getRounds() throws SQLException {
		if (mEvents.getEvent(mEventId) == null) throw new NotFoundException("Event not found.");
		return mEvents.getRounds(mEventId);
	}
	
	@Post
	public CreatedBean addRound() throws SQLException {
		ValidationUtils.verifyGroup(getRequest(), ApiV1.MODERATOR);
		if (mEvents.getEvent(mEventId) == null) throw new NotFoundException("Event not found.");
		return new CreatedBean(mEvents.addRound(mEventId));
	}
}
