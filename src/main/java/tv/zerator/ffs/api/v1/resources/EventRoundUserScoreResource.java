package tv.zerator.ffs.api.v1.resources;

import java.sql.SQLException;

import org.restlet.data.Status;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import alexmog.apilib.exceptions.BadEntityException;
import alexmog.apilib.exceptions.NotFoundException;
import alexmog.apilib.managers.DaoManager.DaoInject;
import tv.zerator.ffs.api.dao.EventsDao;

public class EventRoundUserScoreResource extends ServerResource {
	@DaoInject
	private static EventsDao mEvents;

	private int mEventId, mUserId, mRoundId;
	
	@Override
	protected void doInit() throws ResourceException {
		mEventId = Integer.parseInt(getAttribute("EVENT_ID"));
		mUserId = Integer.parseInt(getAttribute("USER_ID"));
		mRoundId = Integer.parseInt(getAttribute("ROUND_ID"));
	}
	
	@Put
	public Status updateScore(UpdateScoreEntity entity) throws SQLException {
		if (entity == null) throw new BadEntityException("entity not found.");

		if (mEvents.getEvent(mEventId) == null) throw new NotFoundException("Event not found.");
		if (!mEvents.roundExists(mEventId, mRoundId)) throw new NotFoundException("Round not found.");
		if (mEvents.getRegistered(mEventId, mUserId) == null) throw new NotFoundException("User not found on this event.");
		
		if (mEvents.getScore(mRoundId, mUserId) != null) mEvents.updateScore(mRoundId, mUserId, entity.score);
		else mEvents.addScore(mRoundId, mUserId, entity.score);

		return Status.SUCCESS_OK;
	}
	
	private static class UpdateScoreEntity {
		public int score;
	}
}
