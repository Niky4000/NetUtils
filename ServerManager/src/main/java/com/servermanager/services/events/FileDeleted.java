package com.servermanager.services.events;

import java.io.File;
import java.util.Date;

public class FileDeleted extends FileEvent {

	public FileDeleted(File file, Date date) {
		super(file, date);
	}
}
