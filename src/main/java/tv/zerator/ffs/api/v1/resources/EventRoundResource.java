package tv.zerator.ffs.api.v1.resources;

import java.sql.SQLException;
import java.util.List;

import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import alexmog.apilib.exceptions.NotFoundException;
import alexmog.apilib.managers.DaoManager.DaoInject;
import tv.zerator.ffs.api.dao.EventsDao;
import tv.zerator.ffs.api.dao.EventsDao.RoundUserScoreBean;
import tv.zerator.ffs.api.utils.ValidationUtils;
import tv.zerator.ffs.api.v1.ApiV1;

public class EventRoundResource extends ServerResource {
	@DaoInject
	private static EventsDao mEvents;
	
	private int mEventId, mRoundId;

	@Override
	protected void doInit() throws ResourceException {
		mEventId = Integer.parseInt(getAttribute("EVENT_ID"));
		mRoundId = Integer.parseInt(getAttribute("ROUND_ID"));
	}
	
	@Get
	public List<RoundUserScoreBean> getRound() throws SQLException {
		if (!mEvents.roundExists(mEventId, mRoundId)) throw new NotFoundException("ROUND_NOT_FOUND");
		return mEvents.getScores(mRoundId);
	}
	
	@Delete
	public Status deleteRound() throws SQLException {
		ValidationUtils.verifyGroup(getRequest(), ApiV1.ADMIN);
		if (!mEvents.roundExists(mEventId, mRoundId)) throw new NotFoundException("ROUND_NOT_FOUND");
		mEvents.deleteRound(mEventId, mRoundId);
		return Status.SUCCESS_OK;
	}
}
