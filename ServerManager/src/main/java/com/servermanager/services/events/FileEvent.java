package com.servermanager.services.events;

import java.io.File;

public class FileEvent implements Event {

	private final File file;

	public FileEvent(File file) {
		this.file = file;
	}

	public File getFile() {
		return file;
	}
}
