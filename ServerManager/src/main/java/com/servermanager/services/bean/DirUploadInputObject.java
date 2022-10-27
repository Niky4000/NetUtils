package com.servermanager.services.bean;

import com.servermanager.StartServerManager;
import java.io.File;
import java.util.Date;

public class DirUploadInputObject<T> extends FileUploadInputObject<T> {

	public DirUploadInputObject(File file, Date eventDate) {
		super(file, eventDate);
	}

	@Override
	public TransferObject apply(TransferObject<T> object, StartServerManager startServerManager) {
		if (!file.exists()) {
			file.mkdirs();
		}
		return new TransferObject();
	}
}
