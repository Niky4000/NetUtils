package com.servermanager.services.bean;

import com.servermanager.StartServerManager;
import java.io.File;

public class FileDeleteInputObject<T> extends TransferObject<T> {

	private final File file;

	public FileDeleteInputObject(File file) {
		this.file = file;
	}

	@Override
	public TransferObject apply(TransferObject<T> object) {
		file.delete();
		if (file.getParentFile().listFiles().length == 0) {
			file.getParentFile().delete();
			try {
				StartServerManager.getClusterService().fileDeletedEvent(file);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return new TransferObject();
	}
}
