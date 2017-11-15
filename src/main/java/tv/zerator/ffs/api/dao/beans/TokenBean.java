package tv.zerator.ffs.api.dao.beans;

import lombok.Data;

public @Data class TokenBean {
	private int accountId;
	private String token;
	private long lastUsedTimestamp;
}
