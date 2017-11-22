package tv.zerator.ffs.api.v1;

import java.net.URISyntaxException;
import java.util.Properties;

import org.restlet.routing.Router;

import alexmog.apilib.api.ApiBase;
import tv.zerator.ffs.api.v1.authorizers.AuthAggregator;
import tv.zerator.ffs.api.v1.authorizers.GroupAuthorizer;
import tv.zerator.ffs.api.v1.resources.AwaitingForActivationResource;
import tv.zerator.ffs.api.v1.resources.BannedResource;
import tv.zerator.ffs.api.v1.resources.EventCurrentResource;
import tv.zerator.ffs.api.v1.resources.EventRegisterResource;
import tv.zerator.ffs.api.v1.resources.EventResource;
import tv.zerator.ffs.api.v1.resources.EventRoundResource;
import tv.zerator.ffs.api.v1.resources.EventRoundUserScoreResource;
import tv.zerator.ffs.api.v1.resources.EventRoundsResource;
import tv.zerator.ffs.api.v1.resources.EventUserResource;
import tv.zerator.ffs.api.v1.resources.EventUsersResource;
import tv.zerator.ffs.api.v1.resources.EventsResource;
import tv.zerator.ffs.api.v1.resources.LoginResource;
import tv.zerator.ffs.api.v1.resources.MeEventsResource;
import tv.zerator.ffs.api.v1.resources.MeResource;
import tv.zerator.ffs.api.v1.verifiers.OAuthVerifier;

public class ApiV1 extends ApiBase {
	public static final int AWAITING_FOR_ACTIVATION = 0,
			BANNED = 500,
			USER = 1000,
			MODERATOR = 2000,
			ADMIN = 3000,
			SUPERADMIN = 4000;

	public ApiV1(Properties config) throws URISyntaxException {
		super("FFS-API", "FFS API", new OAuthVerifier(), GroupAuthorizer.class, new AuthAggregator(),
				AWAITING_FOR_ACTIVATION, BANNED, USER, MODERATOR, ADMIN, SUPERADMIN);
	}

	@Override
	public void init(Properties config) {}

	@Override
	protected void configurePublicRouter(Router router) {
		router.attach("/account/login", LoginResource.class);
		router.attach("/events", EventsResource.class);
		router.attach("/event/current", EventCurrentResource.class);
		router.attach("/event/{EVENT_ID}", EventResource.class);
		router.attach("/event/{EVENT_ID}/users", EventUsersResource.class);
		router.attach("/event/{EVENT_ID}/user/{USER_ID}", EventUserResource.class);
		router.attach("/event/{EVENT_ID}/rounds", EventRoundsResource.class);
		router.attach("/event/{EVENT_ID}/round/{ROUND_ID}", EventRoundResource.class);
	}

	@Override
	protected void configureRouter(int group, Router router) {
		switch (group) {
		case AWAITING_FOR_ACTIVATION:
			router.attachDefault(AwaitingForActivationResource.class);
			router.attach("/me", MeResource.class);
			router.attach("/me/events", MeEventsResource.class);
			break;
		case BANNED:
			router.attachDefault(BannedResource.class);
			break;
		case USER:
			router.attach("/event/{EVENT_ID}/register", EventRegisterResource.class);
			break;
		case MODERATOR:
			router.attach("/event/{EVENT_ID}/round/{ROUND_ID}/score/{USER_ID}", EventRoundUserScoreResource.class);
			break;
		case ADMIN:
			break;
		case SUPERADMIN:
			break;
		}
	}

	@Override
	protected void configureAuthenticatedRouter(Router router) {}

}
