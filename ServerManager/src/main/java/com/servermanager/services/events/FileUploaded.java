package com.servermanager.services.events;

import java.io.File;

public class FileUploaded extends FileEvent {

	public FileUploaded(File file) {
		super(file);
	}
}
