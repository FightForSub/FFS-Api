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
	public Map<Integer, List<RoundUserScoreBean>> getRounds() throws SQLException {
		if (mEvents.getEvent(mEventId) == null) throw new NotFoundException("EVENT_NOT_FOUND");
		
		Map<Integer, List<RoundUserScoreBean>> rounds = new HashMap<>();
		
		List<Integer> roundIds = mEvents.getRounds(mEventId);

		if (roundIds.isEmpty()) return rounds;
		
		for (int r : roundIds) rounds.put(r, new ArrayList<RoundUserScoreBean>());
		
		List<RoundUserScoreBean> scores = mEvents.getAllScores(mEventId);
		
		for (RoundUserScoreBean bean : scores) {
			List<RoundUserScoreBean> list = rounds.get(bean.getRound());
			if (list == null) {
				list = new ArrayList<>();
				rounds.put(bean.getRound(), list);
			}
			list.add(bean);
		}
		
		return rounds;
	}
	
	@Post
	public CreatedBean addRound() throws SQLException {
		ValidationUtils.verifyGroup(getRequest(), ApiV1.MODERATOR);
		if (mEvents.getEvent(mEventId) == null) throw new NotFoundException("EVENT_NOT_FOUND");
		return new CreatedBean(mEvents.addRound(mEventId));
	}
}
