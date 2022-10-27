package com.servermanager.services;

import com.servermanager.StartServerManager;
import com.servermanager.services.bean.TransferObject;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ClientService extends AbstractService {

	private final StartServerManager startServerManager;

	public ClientService(String host, int port, StartServerManager startServerManager) {
		super(host, port);
		this.startServerManager = startServerManager;
	}

	public List<TransferObject> sendMessage(Iterator<? extends TransferObject> inputObject) throws IOException, ClassNotFoundException {
		List<TransferObject> listToReturn = new ArrayList<>();
		try (Socket socket = new Socket(host, port);
				ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());) {
			TransferObject outputObject = null;
			while (inputObject.hasNext()) {
				TransferObject next = inputObject.next();
				do {
					outputStream.writeObject(next);
					outputStream.flush();
					outputObject = (TransferObject) inputStream.readObject();
					outputObject.apply(null, startServerManager);
					listToReturn.add(outputObject);
				} while (!outputObject.isDeadPill());
			}
		}
		return listToReturn;
	}
}
