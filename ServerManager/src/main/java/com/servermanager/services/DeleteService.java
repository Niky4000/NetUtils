package com.servermanager.services;

import com.servermanager.StartServerManager;
import com.servermanager.services.bean.FileDeleteInputObject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;

public class DeleteService extends AbstractService {

	private final StartServerManager startServerManager;

	public DeleteService(String host, int port, StartServerManager startServerManager) {
		super(host, port);
		this.startServerManager = startServerManager;
	}

	public void delete(Path to, String uuid, Date eventDate) throws IOException, ClassNotFoundException {
		new ClientService(host, port, startServerManager).sendMessage(Arrays.asList(new FileDeleteInputObject<>(to.toFile(), uuid, eventDate)).iterator());
	}
}
