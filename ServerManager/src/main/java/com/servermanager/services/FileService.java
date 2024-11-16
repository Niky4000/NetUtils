package com.servermanager.services;

import com.servermanager.StartServerManager;
import com.servermanager.services.bean.FileListRequestBean;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class FileService extends AbstractService {

	private final StartServerManager startServerManager;

	public FileService(String host, int port, StartServerManager startServerManager) {
		super(host, port);
		this.startServerManager = startServerManager;
	}

	public List<File> getFileList(File dir) throws IOException, ClassNotFoundException {
		FileListRequestBean fileListRequestResponseBean = (FileListRequestBean) new ClientService(host, port, startServerManager).sendMessage(Arrays.asList(new FileListRequestBean<>(dir)).iterator()).get(0);
		return fileListRequestResponseBean.getFileList();
	}
}
