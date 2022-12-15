package com.servermanager.services;

import com.servermanager.StartServerManager;
import com.servermanager.services.bean.FileDownloadOutputObject;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;

public class DownloadService extends AbstractService {

	private final StartServerManager startServerManager;

	public DownloadService(String host, int port, StartServerManager startServerManager) {
		super(host, port);
		this.startServerManager = startServerManager;
	}

	public void download(Path from, Path to, Date eventDate) throws Exception {
		if (to.toFile().exists()) {
			to.toFile().delete();
		}
		new ClientService(host, port, startServerManager).sendMessage(Arrays.asList(new FileDownloadOutputObject<>(from.toFile(), to.toFile(), eventDate)).iterator());
	}
}
