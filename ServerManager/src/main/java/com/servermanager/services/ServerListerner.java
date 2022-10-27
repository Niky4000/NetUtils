package com.servermanager.services;

import com.servermanager.StartServerManager;
import com.servermanager.services.bean.TransferObject;
import com.utils.WaitUtils;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerListerner {

	private final int port;
	private AtomicBoolean shutdown = new AtomicBoolean(false);
	private static final int TIME_TO_WAIT = 60;
	private final StartServerManager startServerManager;

	public ServerListerner(int port, StartServerManager startServerManager) {
		this.port = port;
		this.startServerManager = startServerManager;
	}

	public void listen() throws IOException {
		Thread serverListernerThread = new Thread(() -> {
			while (true) {
				try {
					ServerSocket serverSocket = new ServerSocket(port);
					while (!shutdown.get()) {
						Socket socket = serverSocket.accept();
						socketHandler(socket);
					}
				} catch (Exception e) {
					WaitUtils.waitSomeTime(TIME_TO_WAIT);
					continue;
				}
			}
		});
		serverListernerThread.setName("serverListernerThread");
		serverListernerThread.start();
	}

	private void socketHandler(Socket socket) {
		TransferObject globalInputObject = null;
		try {
			try (ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
					ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());) {
				do {
					TransferObject inputObject = (TransferObject) inputStream.readObject();
					TransferObject outputObject = inputObject.apply(globalInputObject, startServerManager);
					globalInputObject = inputObject;
					outputStream.writeObject(outputObject);
					outputStream.flush();
				} while (!globalInputObject.isDeadPill());
			}
		} catch (EOFException eofException) {
			// Ignore it!
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			exceptionHandler(globalInputObject);
			try {
				if (!socket.isClosed()) {
					socket.close();
				}
			} catch (Exception ee) {
				// Ignore it!
			}
		}
	}

	private void exceptionHandler(TransferObject inputObject) {
		if (inputObject != null) {
			inputObject.finish();
		}
	}

	public void shutdown() {
		try {
			shutdown.set(true);
			Socket socket = new Socket("127.0.0.1", port);
			socket.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
