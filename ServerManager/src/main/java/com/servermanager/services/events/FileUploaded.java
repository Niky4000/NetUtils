package com.servermanager.services.events;

import java.io.File;
import java.util.Date;

public class FileUploaded extends FileEvent {

	public FileUploaded(File file, Date date) {
		super(file,date );
	}
}
