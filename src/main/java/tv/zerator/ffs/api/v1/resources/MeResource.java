package tv.zerator.ffs.api.v1.resources;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import tv.zerator.ffs.api.dao.beans.AccountBean;

public class MeResource extends ServerResource {
	@Get
	public AccountBean getMe() {
		return (AccountBean) getRequest().getAttributes().get("account");
	}
}
