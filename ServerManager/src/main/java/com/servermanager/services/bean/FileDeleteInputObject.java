package com.servermanager.services.bean;

import com.servermanager.StartServerManager;
import java.io.File;
import java.util.Date;

public class FileDeleteInputObject<T> extends TransferObject<T> {

	private final File file;
	private final Date eventDate;

	public FileDeleteInputObject(File file, Date eventDate) {
		this.file = file;
		this.eventDate = eventDate;
	}

	@Override
	public TransferObject apply(TransferObject<T> object) {
		file.delete();
		try {
			StartServerManager.getClusterService().fileDeletedEvent(file, eventDate);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (file.getParentFile().listFiles().length == 0) {
			file.getParentFile().delete();
		}
		return new TransferObject();
	}
}
