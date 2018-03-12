package tv.zerator.ffs.api.dao.beans;

import lombok.Data;

public @Data class RoundScoreBean {
	private int roundId, accountId;
	private double score;
}
