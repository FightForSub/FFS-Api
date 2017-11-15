package tv.zerator.ffs.api.v1.authorizers;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Method;
import org.restlet.security.Authorizer;

import tv.zerator.ffs.api.dao.beans.AccountBean;

public class GroupAuthorizer extends Authorizer {
	private final int mMinimalGrade;

	public GroupAuthorizer(int minimalGrade) {
		mMinimalGrade = minimalGrade;
	}
	
	@Override
	protected boolean authorize(Request request, Response response) {
		if (request.getMethod() == Method.OPTIONS) return true;
		AccountBean bean = (AccountBean) request.getAttributes().get("account");
		return bean.getGrade() >= mMinimalGrade;
	}
}
