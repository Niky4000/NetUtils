package com.servermanager.services.events;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class Event implements Serializable {

	private final Date date;
	private final String uuid;

	public Event() {
		this.date = new Date();
		this.uuid = UUID.randomUUID().toString();
	}

	public Event(String uuid, Date date) {
		this.date = date;
		this.uuid = uuid;
	}

	public Date getDate() {
		return date;
	}

	public String getUuid() {
		return uuid;
	}
}
