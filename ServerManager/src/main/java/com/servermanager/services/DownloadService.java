package com.servermanager.services;

import com.servermanager.services.bean.FileDownloadOutputObject;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;

public class DownloadService extends AbstractService {

	public DownloadService(String host, int port) {
		super(host, port);
	}

	public void download(Path from, Path to, Date eventDate) throws Exception {
		if (to.toFile().exists()) {
			to.toFile().delete();
		}
		new ClientService(host, port).sendMessage(Arrays.asList(new FileDownloadOutputObject<>(from.toFile(), to.toFile(), eventDate)).iterator());
	}
}
