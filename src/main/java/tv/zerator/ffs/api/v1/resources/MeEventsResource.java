package tv.zerator.ffs.api.v1.resources;

import java.sql.SQLException;
import java.util.List;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import alexmog.apilib.managers.DaoManager.DaoInject;
import tv.zerator.ffs.api.dao.EventsDao;
import tv.zerator.ffs.api.dao.EventsDao.AccountEventBean;
import tv.zerator.ffs.api.dao.EventsDao.UserStatus;
import tv.zerator.ffs.api.dao.beans.AccountBean;

public class MeEventsResource extends ServerResource {
	@DaoInject
	private static EventsDao mEvents;
	
	@Get
	public List<AccountEventBean> getEvents() throws SQLException {
		String statusStr = getQuery().getFirstValue("status");
		UserStatus status = UserStatus.VALIDATED;
		try {
			if (statusStr != null) status = UserStatus.valueOf(statusStr);
		} catch (Exception e) {}
		return statusStr == null ? mEvents.getEventsForAccount(((AccountBean) getRequest().getAttributes().get("account")).getTwitchId())
				: mEvents.getEventsForAccountAndStatus(((AccountBean) getRequest().getAttributes().get("account")).getTwitchId(), status);
	}
}
