package tv.zerator.ffs.api.dao.beans;

import lombok.Data;

public @Data class AccountBean {
	private int twitchId, views, followers, grade;
	private String username, email, url, emailActivationKey, logo;
	private BroadcasterType broadcasterType;
	
	public enum BroadcasterType {
		affiliate,
		partner,
		none
	}
}
