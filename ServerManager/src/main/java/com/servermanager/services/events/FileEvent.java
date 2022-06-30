package com.servermanager.services.events;

import java.io.File;

public class FileEvent extends Event {

	private final File file;

	public FileEvent(File file) {
		super();
		this.file = file;
	}

	public File getFile() {
		return file;
	}
}
