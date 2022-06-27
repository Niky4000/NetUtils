package com.servermanager.services.bean;

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
		}
		return new TransferObject();
	}
}
