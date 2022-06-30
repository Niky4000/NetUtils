package com.servermanager.services.events;

import java.io.File;

public class FileDeleted extends FileEvent {

	public FileDeleted(File file) {
		super(file);
	}
}
