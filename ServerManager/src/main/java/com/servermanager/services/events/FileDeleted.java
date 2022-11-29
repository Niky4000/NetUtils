package com.servermanager.services.events;

import java.io.File;
import java.util.Date;
import java.util.UUID;

public class FileDeleted extends FileEvent {

	public FileDeleted(File file, String uuid, Date date) {
		super(file, uuid, date);
	}

	public FileDeleted(File file, Date date) {
		super(file, UUID.randomUUID().toString(), date);
	}
}
