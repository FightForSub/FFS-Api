package tv.zerator.ffs.api.v1.authorizers;

import java.util.logging.Level;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Method;
import org.restlet.security.Authorizer;

import com.google.common.net.HttpHeaders;

import alexmog.apilib.managers.DaoManager.DaoInject;
import tv.zerator.ffs.api.Main;
import tv.zerator.ffs.api.dao.AccountsDao;
import tv.zerator.ffs.api.dao.beans.AccountBean;

public class AuthAggregator extends Authorizer {
	@DaoInject
	private static AccountsDao mAccounts;

	@Override
	protected boolean authorize(Request request, Response response) {
		if (request.getMethod() == Method.OPTIONS) return true;

		if (request.getAttributes().get("account") != null) return true;
		
		String accessToken = request.getHeaders().getFirstValue(HttpHeaders.AUTHORIZATION);
		if (accessToken == null) return true;
		
		try {
			accessToken.replace("OAuth ", "");
			AccountBean acc = mAccounts.getAccountFromToken(accessToken);
		    if (acc != null) {
		    	request.getAttributes().put("account", acc);
		    	return true;
		    }
		} catch (Exception e) {
			Main.LOGGER.log(Level.WARNING, "Error while handling OAuth authentification", e);
			return false;
		}
	    
		return false;
	}

}
