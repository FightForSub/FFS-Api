package tv.zerator.ffs.api.dao.beans;

import lombok.Data;

public @Data class EventBean {
	private int id;
	private String name, description;
	private boolean reservedToAffiliates, reservedToPartners, isCurrent;
	private Status status;
	
	public enum Status {
		OPEN,
		CLOSED,
		STARTED,
		ENDED
	}
}
