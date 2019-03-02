package tv.zerator.ffs.api.v1.resources;

import alexmog.apilib.api.validation.ValidationErrors;
import alexmog.apilib.exceptions.BadAuthenticationException;
import alexmog.apilib.exceptions.BadEntityException;
import alexmog.apilib.exceptions.ConflictException;
import alexmog.apilib.exceptions.NotFoundException;
import alexmog.apilib.exceptions.InternalServerError;
import alexmog.apilib.managers.DaoManager;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.restlet.data.Status;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import tv.zerator.ffs.api.Main;
import tv.zerator.ffs.api.dao.EventsDao;
import tv.zerator.ffs.api.dao.beans.AccountBean;
import tv.zerator.ffs.api.dao.beans.EventBean;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.logging.Level;

public class EventCheckSubscriptionResource extends ServerResource {
    @DaoManager.DaoInject
    private static EventsDao mEvents;

    private int mEventId;

    private final CloseableHttpClient mHttpClient = HttpClients.createDefault();
    private static String mTwitchUsersUrl = null, mTwitchClientId, mTwitchAccept;

    public EventCheckSubscriptionResource() {
        if (mTwitchUsersUrl != null) return;
        mTwitchUsersUrl = Main.getInstance().getConfig().getProperty("twitch.users_url");
        mTwitchClientId = Main.getInstance().getConfig().getProperty("twitch.client_id");
        mTwitchAccept = Main.getInstance().getConfig().getProperty("twitch.accept_header");
    }

    @Override
    protected void doInit() throws ResourceException {
        mEventId = Integer.parseInt(getAttribute("EVENT_ID"));
    }

    @Post
    public Status checkSubscription(LoginBean entity) throws SQLException {
        EventBean event = mEvents.getEvent(mEventId);
        if (event == null) throw new NotFoundException("EVENT_NOT_FOUND");

        if (event.getStatus() != EventBean.Status.ENDED) throw new ConflictException("EVENT_NOT_ENDED");

        AccountBean acc = (AccountBean) getRequest().getAttributes().get("account");

        ValidationErrors err = new ValidationErrors();

        if (entity == null) throw new BadEntityException("Entity not found.");

        err.verifyFieldEmptyness("twitch_token", entity.twitch_token, 1);

        err.checkErrors("LOGIN_ERROR");

        AccountBean winnerAccount = mEvents.getUserFromRank(mEventId,1);

        if (winnerAccount == null){
            throw new ConflictException("WINNER_NOT_DEFINED");
        }

        int retCode;
        try {
            HttpGet httpGet = new HttpGet(new URI(mTwitchUsersUrl + acc.getTwitchId() + "/subscriptions/" + winnerAccount.getTwitchId()));

            httpGet.addHeader("Accept", mTwitchAccept);
            httpGet.addHeader("Client-ID", mTwitchClientId);
            httpGet.addHeader("Authorization", "OAuth " + entity.twitch_token);

            try (CloseableHttpResponse resp = mHttpClient.execute(httpGet, HttpClientContext.create());) {
                retCode = resp.getStatusLine().getStatusCode();
            }

            httpGet.releaseConnection();

            switch (retCode){
                case 404:
                    throw new NotFoundException("USER_NOT_SUBSCRIBE");

                case 403:
                    throw new BadAuthenticationException("TOKEN_NOT_ALLOW_TO_CHECK_SUBSCRIPTION");

                case 200:
                    mEvents.updateUserSubscriptionToWinner(mEventId,acc.getTwitchId(),true);
                    return Status.SUCCESS_OK;

                default:
                    throw new BadAuthenticationException("BAD_AUTHENTICATION");
            }

        } catch (URISyntaxException | IOException e) {
            Main.LOGGER.log(Level.SEVERE, "Impossible to login", e);
            throw new InternalServerError("TWITCH_API_ERROR");
        }
    }

    private static class LoginBean {
        public String twitch_token;
    }
}
