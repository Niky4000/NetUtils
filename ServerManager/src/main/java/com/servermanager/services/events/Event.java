package com.servermanager.services.events;

import java.io.Serializable;
import java.util.Date;

public class Event implements Serializable {

	private final Date date;

	public Event() {
		this.date = new Date();
	}

	public Date getDate() {
		return date;
	}
}
