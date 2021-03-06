package com.servermanager.services;

import com.servermanager.services.bean.FileDeleteInputObject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;

public class DeleteService extends AbstractService {

	public DeleteService(String host, int port) {
		super(host, port);
	}

	public void delete(Path to, Date eventDate) throws IOException, ClassNotFoundException {
		new ClientService(host, port).sendMessage(Arrays.asList(new FileDeleteInputObject<>(to.toFile(), eventDate)).iterator());
	}
}
