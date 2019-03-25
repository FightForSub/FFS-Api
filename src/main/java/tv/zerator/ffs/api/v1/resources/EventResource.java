package tv.zerator.ffs.api.v1.resources;

import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Comparator;


import lombok.RequiredArgsConstructor;
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
		if (event == null) throw new NotFoundException("EVENT_NOT_FOUND");
		return event;
	}

	@Put
	public Status updateEvent(UpdateEventEntity entity) throws SQLException {
		ValidationUtils.verifyGroup(getRequest(), ApiV1.ADMIN);
		ValidationErrors err = new ValidationErrors();

		if (entity == null) throw new BadEntityException("ENTITY_NOT_FOUND");

		err.verifyFieldEmptyness("name", entity.name, 3, 200);
		err.verifyFieldEmptyness("description", entity.description, 3, 2048);

		err.checkErrors("EVENT_UPDATE_ERROR");

		EventBean bean = mEvents.getEvent(mEventId);

		if (entity.status == EventBean.Status.ENDED && bean.getStatus() != EventBean.Status.ENDED){
            rankAllUsers(mEventId);
        }

		if (bean == null) throw new NotFoundException("EVENT_NOT_FOUND");
		bean.setCurrent(entity.current);
		bean.setDescription(entity.description);
		bean.setName(entity.name);
		bean.setReservedToAffiliates(entity.reserved_to_affiliates);
		bean.setReservedToPartners(entity.reserved_to_partners);
		bean.setStatus(entity.status);
		bean.setMinimumViews(entity.minimum_views);
		bean.setMinimumFollowers(entity.minimum_followers);
		bean.setRankingType(entity.ranking_type);
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
		public int minimum_views, minimum_followers;
		public EventBean.Status status;
		public EventBean.RankingType ranking_type;
	}

	private void rankAllUsers(int eventId) throws SQLException {
		EventBean event = mEvents.getEvent(mEventId);
		List<EventsDao.RoundUserScoreBean> allScores = mEvents.getAllScores(eventId);

		Set<Integer> lastUsers = new HashSet<>();

		Map<Integer,Double> map = new HashMap<>();

		for (EventsDao.RoundUserScoreBean rus:
				allScores) {
			if (rus.getScore() == 0){
				lastUsers.add(rus.getId());
			}
			if (map.containsKey(rus.getId())){
				map.put(rus.getId(),map.get(rus.getId()) + rus.getScore());
			} else {
				map.put(rus.getId(),rus.getScore());
			}
		}

		for (Integer userId:
			 lastUsers) {
			map.remove(userId);
		}

		List<Map.Entry<Integer,Double>> list = new ArrayList<>(map.entrySet());
		Collections.sort(list, new ScoreUserValueComparator(event.getRankingType()));

		int i = 1;
		int currentRank = 1;
		Double currentScore = null ;
		for (Map.Entry<Integer,Double> entry:
				list) {

			if (currentScore == null || !currentScore.equals(entry.getValue())){
				currentRank = i;
				currentScore = entry.getValue();
			}

			mEvents.updateUserRank(mEventId, entry.getKey(), currentRank);

			i++;
		}

		for (Integer userId:
			 lastUsers) {
			mEvents.updateUserRank(mEventId, userId, i);
		}

	}

	@RequiredArgsConstructor
	private static class ScoreUserValueComparator implements Comparator<Map.Entry<Integer, Double>> {
		private final EventBean.RankingType rankingType;

		@Override
		public int compare(Map.Entry<Integer, Double> e1, Map.Entry<Integer, Double> e2) {
			if (rankingType.equals(EventBean.RankingType.SCORE_ASC)){
				return Double.compare(e1.getValue(),e2.getValue());
			} else {
				return Double.compare(e2.getValue(),e1.getValue());
			}
		}
	}
}
