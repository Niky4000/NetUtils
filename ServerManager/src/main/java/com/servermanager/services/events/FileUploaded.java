package com.servermanager.services.events;

import java.io.File;
import java.util.Date;
import java.util.UUID;

public class FileUploaded extends FileEvent {

	public FileUploaded(File file, String uuid, Date date) {
		super(file, uuid, date);
	}

	public FileUploaded(File file, Date date) {
		super(file, UUID.randomUUID().toString(), date);
	}
}
