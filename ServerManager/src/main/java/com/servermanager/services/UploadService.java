package com.servermanager.services;

import com.servermanager.StartServerManager;
import com.servermanager.services.bean.DirUploadInputObject;
import com.servermanager.services.bean.FileUploadInputObject;
import com.utils.FileUtils;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

public class UploadService extends AbstractService {

	private final StartServerManager startServerManager;

	public UploadService(String host, int port, StartServerManager startServerManager) {
		super(host, port);
		this.startServerManager = startServerManager;
	}

	public void upload(Path to, Path from, String uuid, Date eventDate) throws Exception {
		if (from.toFile().isDirectory()) {
			new ClientService(host, port, startServerManager).sendMessage(Arrays.asList(new DirUploadInputObject<>(to.toFile(), uuid, eventDate)).iterator());
		} else {
			try (FileInputStream inputStream = new FileInputStream(from.toFile());) {
				new ClientService(host, port, startServerManager).sendMessage(Arrays.asList(new FileUploadInputObject<>(to.toFile(), uuid, eventDate)).iterator());
				Iterator<FileUploadInputObject> iterator = FileUtils.getFileUploadInputObjectIterator(to.toFile(), inputStream, uuid, eventDate, startServerManager);
				new ClientService(host, port, startServerManager).sendMessage(iterator);
			}
		}
	}
}
