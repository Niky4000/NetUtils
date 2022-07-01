package com.servermanager.services;

import com.servermanager.services.bean.FileUploadInputObject;
import com.utils.FileUtils;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

public class UploadService extends AbstractService {

	public UploadService(String host, int port) {
		super(host, port);
	}

	public void upload(Path to, Path from, Date eventDate) throws Exception {
		try (FileInputStream inputStream = new FileInputStream(from.toFile());) {
			new ClientService(host, port).sendMessage(Arrays.asList(new FileUploadInputObject<>(to.toFile(), eventDate)).iterator());
			Iterator<FileUploadInputObject> iterator = FileUtils.getFileUploadInputObjectIterator(to.toFile(), inputStream, eventDate);
			new ClientService(host, port).sendMessage(iterator);
		}
	}
}
