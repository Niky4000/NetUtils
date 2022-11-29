package com.servermanager.services.events;

import java.io.File;
import java.util.Date;

public class FileEvent extends Event {

	private final File file;

	public FileEvent(File file, String uuid, Date date) {
		super(uuid, date);
		this.file = file;
	}

	public File getFile() {
		return file;
	}
}
