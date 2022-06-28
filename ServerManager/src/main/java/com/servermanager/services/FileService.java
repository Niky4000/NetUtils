package com.servermanager.services;

import com.servermanager.services.bean.FileListRequestBean;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class FileService extends AbstractService {

	public FileService(String host, int port) {
		super(host, port);
	}

	public List<File> getFileList(File dir) throws IOException, ClassNotFoundException {
		FileListRequestBean fileListRequestResponseBean = (FileListRequestBean) new ClientService(host, port).sendMessage(Arrays.asList(new FileListRequestBean<>(dir)).iterator()).get(0);
		return fileListRequestResponseBean.getFileList();
	}
}
