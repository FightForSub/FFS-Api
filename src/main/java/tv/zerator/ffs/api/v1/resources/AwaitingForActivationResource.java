package tv.zerator.ffs.api.v1.resources;

import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import alexmog.apilib.exceptions.PermissionsException;

public class AwaitingForActivationResource extends ServerResource {
	@Override
	protected void doInit() throws ResourceException {
		throw new PermissionsException("Your account is awaiting for activation. Please check your emails.");
	}
}