package com.servermanager.services;

import com.servermanager.StartServerManager;
import com.servermanager.services.bean.EventObject;
import com.servermanager.services.bean.TransferObject;
import static com.utils.Logger.println;
import com.utils.WaitUtils;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
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

	public void sendInterruptionsToTheRemoteEventListerners() {
		Iterator<Entry<Socket, ObjectOutputStream>> iterator = openedEventSockets.iterator();
		while (iterator.hasNext()) {
			Entry<Socket, ObjectOutputStream> next = iterator.next();
			try {
				println("One socket was closed!");
				Socket socket = next.getKey();
				ObjectOutputStream outputStream = next.getValue();
				outputStream.writeObject(new EventObject());
				outputStream.flush();
				socket.close();
			} catch (IOException ex) {
				// Ignore it!
			}
		}
	}

	private void socketHandler(Socket socket) {
		new Thread(() -> {
			TransferObject globalInputObject = null;
			try {
				try (ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
						ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());) {
					do {
						TransferObject inputObject = (TransferObject) inputStream.readObject();
						if (inputObject instanceof EventObject) {
							globalInputObject = inputObject;
							handleEventObject(socket, inputStream, outputStream, (EventObject) inputObject);
							break;
						}
						TransferObject outputObject = inputObject.apply(globalInputObject, startServerManager);
						globalInputObject = inputObject;
						outputStream.writeObject(outputObject);
						outputStream.flush();
					} while (!globalInputObject.isDeadPill());
				}
			} catch (EOFException eofException) {
				// Ignore it!
			} catch (Exception e) {
				if (globalInputObject != null && !(globalInputObject instanceof EventObject)) {
					println(e);
				}
			} finally {
				if (globalInputObject != null && !(globalInputObject instanceof EventObject)) {
					finalizationAction(globalInputObject, socket);
				}
			}
		}).start();
	}

	private void finalizationAction(TransferObject globalInputObject, Socket socket) {
		exceptionHandler(globalInputObject);
		try {
			if (!socket.isClosed()) {
				socket.close();
			}
		} catch (Exception ee) {
			// Ignore it!
		}
	}

	private Object monitor = new Object();
	private static final int READ_TIMEOUT = 1000;
	private List<Entry<Socket, ObjectOutputStream>> openedEventSockets = new CopyOnWriteArrayList<>();

	private void handleEventObject(Socket socket, ObjectInputStream inputStream, ObjectOutputStream outputStream, EventObject eventObject) throws SocketException {
		socket.setSoTimeout(READ_TIMEOUT);
		openedEventSockets.add(new AbstractMap.SimpleEntry<>(socket, outputStream));
		while (true) {
			try {
				Object readObject = inputStream.readObject();
			} catch (SocketTimeoutException timeout) {
				continue;
			} catch (Exception ex) {
				TransferObject outputObject = eventObject.apply(eventObject, startServerManager);
				try {
					outputStream.writeObject(outputObject);
					outputStream.flush();
				} catch (Exception e) {
					break;
				} finally {
					finalizationAction(eventObject, socket);
				}
				break;
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
