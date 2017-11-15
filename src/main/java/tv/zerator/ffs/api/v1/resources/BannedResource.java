package tv.zerator.ffs.api.v1.resources;

import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import alexmog.apilib.exceptions.PermissionsException;

public class BannedResource extends ServerResource {
	@Override
	protected void doInit() throws ResourceException {
		throw new PermissionsException("You are currently banned. Please contact the support.");
	}
}

