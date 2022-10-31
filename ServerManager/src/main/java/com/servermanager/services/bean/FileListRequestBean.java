package com.servermanager.services.bean;

import com.servermanager.StartServerManager;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class FileListRequestBean<T> extends TransferObject<T> {

	private final File dir;
	private final List<File> fileList;
	private final boolean request;

	public FileListRequestBean(File dir) {
		this.dir = dir;
		this.fileList = null;
		this.request = true;
	}

	public FileListRequestBean(File dir, List<File> fileList) {
		this.dir = dir;
		this.fileList = fileList;
		this.request = false;
	}

	@Override
	public TransferObject apply(TransferObject<T> object, StartServerManager startServerManager) {
		if (request) {
			if (dir.exists()) {
				dir.mkdirs();
			}
			return new FileListRequestBean(dir, Arrays.asList(dir.listFiles()));
		} else {
			return this;
		}
	}

	public List<File> getFileList() {
		return fileList;
	}
}
