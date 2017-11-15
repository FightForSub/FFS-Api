package tv.zerator.ffs.api.v1.verifiers;

import java.util.logging.Level;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Method;
import org.restlet.security.Verifier;

import com.google.common.net.HttpHeaders;

import alexmog.apilib.managers.DaoManager.DaoInject;
import tv.zerator.ffs.api.Main;
import tv.zerator.ffs.api.dao.AccountsDao;
import tv.zerator.ffs.api.dao.beans.AccountBean;

public class OAuthVerifier implements Verifier {
	@DaoInject
	private static AccountsDao mAccounts;

	@Override
	public int verify(Request request, Response response) {
		if (request.getMethod() == Method.OPTIONS) return RESULT_VALID;

		if (request.getAttributes().get("account") != null) return RESULT_VALID;
		
		String accessToken = request.getHeaders().getFirstValue(HttpHeaders.AUTHORIZATION);
		if (accessToken == null) return RESULT_MISSING;
		
		try {
			accessToken.replace("OAuth ", "");
			AccountBean acc = mAccounts.getAccountFromToken(accessToken);
		    if (acc != null) {
		    	request.getAttributes().put("account", acc);
		    	return RESULT_VALID;
		    }
		} catch (Exception e) {
			Main.LOGGER.log(Level.WARNING, "Error while handling OAuth authentification", e);
			return RESULT_INVALID;
		}
	    
		return RESULT_INVALID;
	}
}
