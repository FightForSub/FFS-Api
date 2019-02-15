package tv.zerator.ffs.api.v1.resources;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import alexmog.apilib.exceptions.NotFoundException;
import alexmog.apilib.managers.DaoManager.DaoInject;
import tv.zerator.ffs.api.dao.EventsDao;
import tv.zerator.ffs.api.dao.EventsDao.RoundUserScoreBean;
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
	public List<Round> getRounds() throws SQLException {
		if (mEvents.getEvent(mEventId) == null) throw new NotFoundException("EVENT_NOT_FOUND");
		
		List<RoundUserScoreBean> scores = mEvents.getAllScores();
		
		List<Round> ret = new ArrayList<>();
		
		Map<Integer, Round> roundsCache = new HashMap<>();
		for (RoundUserScoreBean bean : scores) {
			Round round = roundsCache.get(bean.getRound());
			if (round == null) {
				round = new Round();
				round.round = bean.getRound();
				roundsCache.put(round.round, round);
				ret.add(round);
			}
			round.scores.add(bean);
		}
		
		return ret;
	}
	
	@Post
	public CreatedBean addRound() throws SQLException {
		ValidationUtils.verifyGroup(getRequest(), ApiV1.MODERATOR);
		if (mEvents.getEvent(mEventId) == null) throw new NotFoundException("EVENT_NOT_FOUND");
		return new CreatedBean(mEvents.addRound(mEventId));
	}
	
	public static class Round {
		public int round;
		public List<RoundUserScoreBean> scores = new ArrayList<>();
	}
}
