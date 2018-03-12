package tv.zerator.ffs.api.v1.resources;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

import org.restlet.data.Status;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import alexmog.apilib.Server;
import alexmog.apilib.exceptions.BadEntityException;
import alexmog.apilib.exceptions.NotFoundException;
import alexmog.apilib.managers.DaoManager.DaoInject;
import alexmog.apilib.managers.Managers.Manager;
import alexmog.apilib.managers.RabbitMQManager;
import lombok.Data;
import tv.zerator.ffs.api.dao.EventsDao;
import tv.zerator.ffs.api.rabbitmq.TopicPacket;

public class EventRoundUserScoreResource extends ServerResource {
	@Manager
	private static RabbitMQManager mRabbitMQManager;
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
		if (entity == null) throw new BadEntityException("ENTITY_NOT_FOUND");

		if (mEvents.getEvent(mEventId) == null) throw new NotFoundException("EVENT_NOT_FOUND");
		if (!mEvents.roundExists(mEventId, mRoundId)) throw new NotFoundException("ROUND_NOT_FOUND");
		if (mEvents.getRegistered(mEventId, mUserId) == null) throw new NotFoundException("USER_NOT_REGISTERED");
		
		if (mEvents.getScore(mRoundId, mUserId) != null) mEvents.updateScore(mRoundId, mUserId, entity.score);
		else mEvents.addScore(mRoundId, mUserId, entity.score);
		
		try {
			mRabbitMQManager.basicPublish("Pub", "", null, new TopicPacket("event-score-v1." + mEventId, new UpdateScoreMessage(mEventId, mRoundId, mUserId, entity.score)));
		} catch (IOException e) {
			Server.LOGGER.log(Level.SEVERE, "Cannot send publish to RabbitMQ", e);
		}

		return Status.SUCCESS_OK;
	}
	
	private @Data static class UpdateScoreMessage {
		public final int event_id, round_id, user_id;
		public final double score;
	}
	
	private static class UpdateScoreEntity {
		public double score;
	}
}
