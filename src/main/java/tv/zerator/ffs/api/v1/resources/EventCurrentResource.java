package tv.zerator.ffs.api.v1.resources;

import java.sql.SQLException;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import alexmog.apilib.exceptions.NotFoundException;
import alexmog.apilib.managers.DaoManager.DaoInject;
import tv.zerator.ffs.api.dao.EventsDao;
import tv.zerator.ffs.api.dao.beans.EventBean;

public class EventCurrentResource extends ServerResource {
	@DaoInject
	private static EventsDao mEvents;
	
	@Get
	public EventBean getCurrent() throws SQLException {
		EventBean bean = mEvents.getCurrent();
		if (bean == null) throw new NotFoundException("NO_EVENT");
		return bean;
	}
}
