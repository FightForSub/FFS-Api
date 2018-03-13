package tv.zerator.ffs.api.v1.resources;

import alexmog.apilib.exceptions.BadEntityException;
import alexmog.apilib.exceptions.NotFoundException;
import alexmog.apilib.managers.DaoManager;
import lombok.Data;
import org.restlet.data.Status;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import tv.zerator.ffs.api.dao.EventsDao;

import java.sql.SQLException;

public class EventUserRankResource extends ServerResource {

    @DaoManager.DaoInject
    private static EventsDao mEvents;

    private int mEventId, mUserId;

    @Override
    protected void doInit() throws ResourceException {
        mEventId = Integer.parseInt(getAttribute("EVENT_ID"));
        mUserId = Integer.parseInt(getAttribute("USER_ID"));
    }

    @Put
    public Status updateRank(UpdateRankEntity entity) throws SQLException {
        if (entity == null) throw new BadEntityException("ENTITY_NOT_FOUND");

        if (mEvents.getEvent(mEventId) == null) throw new NotFoundException("EVENT_NOT_FOUND");
        if (mEvents.getRegistered(mEventId, mUserId) == null) throw new NotFoundException("USER_NOT_REGISTERED");

        mEvents.updateUserRank(mEventId, mUserId, entity.rank);

        return Status.SUCCESS_OK;
    }

    private @Data static class UpdateRankMessage {
        public final int event_id, user_id, rank;
    }

    private static class UpdateRankEntity {
        public int rank;
    }
}
