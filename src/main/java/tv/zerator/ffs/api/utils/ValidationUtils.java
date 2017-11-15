package tv.zerator.ffs.api.utils;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.restlet.Request;

import alexmog.apilib.exceptions.PermissionsException;
import tv.zerator.ffs.api.dao.beans.AccountBean;

public class ValidationUtils {
    private static Pattern pattern = Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
        + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");
    
    public static boolean checkEmail(String email) {
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }
    
    public static void verifyGroup(Request request, int group) throws PermissionsException {
    	if (!hasGroup(request, group)) throw new PermissionsException("You need more permissions to do that!");
    }
    
    private static boolean hasGroup(Request request, int group) {
    	Object obj = request.getAttributes().get("account");
    	if (obj == null) return false;
    	AccountBean account = (AccountBean) obj;
    	return hasGroup(account, group);
    }
    
    private static boolean hasGroup(AccountBean account, int group) {
    	return account.getGrade() >= group;
    }
}