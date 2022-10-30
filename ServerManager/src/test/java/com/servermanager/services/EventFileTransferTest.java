package com.servermanager.services;

import com.servermanager.StartServerManager;
import com.servermanager.services.bean.EventObject;
import com.servermanager.services.bean.TransferObject;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;

public class EventFileTransferTest {

	@Test
	public void test() throws Exception {
		Thread server = th(b -> StartServerManager.main(new String[]{"START", "-port", "4444", "@", "CLUSTER", "-host1", "127.0.0.1", "-home", "/home/me/tmp/shared_observable_dir", "-instanceName", "first", "-port", "48500", "-endPort", "48510", "-localPort", "48100", "-clientPort", "48200", "-clientPortRange", "10"}), "server");
		server.join(10 * 1000);
		Thread client1 = th(b -> StartServerManager.main(new String[]{"WATCH", "-host", "127.0.0.1", "-port", "4444", "-dir", "/home/me/tmp/shared_dir1", "-to", "/home/me/tmp/shared_observable_dir", "@", "EVENT", "-host", "127.0.0.1", "-port", "4444", "-dir", "/home/me/tmp/shared_dir1", "-instanceName", "firstClient", "-clientPort", "48200", "-portRange", "10"}), "client1");
		Thread client2 = th(b -> StartServerManager.main(new String[]{"WATCH", "-host", "127.0.0.1", "-port", "4444", "-dir", "/home/me/tmp/shared_dir2", "-to", "/home/me/tmp/shared_observable_dir", "@", "EVENT", "-host", "127.0.0.1", "-port", "4444", "-dir", "/home/me/tmp/shared_dir2", "-instanceName", "firstClient", "-clientPort", "48200", "-portRange", "10"}), "client2");
		server.join();
	}

	@Test
	public void test2() throws Exception {
		Thread server = th(b -> StartServerManager.main(new String[]{"START", "-port", "4444", "@", "CLUSTER", "-host1", "127.0.0.1", "-home", "/home/me/tmp/shared_observable_dir", "-instanceName", "first", "-port", "48500", "-endPort", "48510", "-localPort", "48100", "-clientPort", "48200", "-clientPortRange", "10"}), "server");
		server.join(10 * 1000);
		Thread client = th(b -> StartServerManager.main(new String[]{"EVENT", "-host", "127.0.0.1", "-port", "4444", "-dir", "/home/me/tmp/shared_dir1", "-instanceName", "firstClient", "-clientPort", "48200", "-portRange", "10"}), "client");
		server.join();
	}

	@Test
	public void test3() throws Exception {
		Thread eventThread = new Thread(() -> {
			while (true) {
				try (Socket socket = new Socket("127.0.0.1", port);
						ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
						BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream());) {
					outputStream.writeObject(new EventObject());
					outputStream.flush();
					byte[] buffer = new byte[16];
					int read = inputStream.read(buffer);
					int read2 = inputStream.read(buffer);
//					Object readObject = inputStream.readObject();
//					if (readObject != null && readObject instanceof EventObject) {
//					threadToInterrupt.interrupt();
//					}
					wait10Seconds();
				} catch (Exception ex) {
					ex.printStackTrace();
					wait10Seconds();
				}
			}
		});
		eventThread.setName("eventThread");
		eventThread.start();
		Thread serverThread = new Thread(() -> {
			try {
				ServerSocket serverSocket = new ServerSocket(port);
				while (true) {
					try {
						Socket socket = serverSocket.accept();
						try (ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
								ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());) {
							handleEventObject(socket, inputStream, outputStream);
						}
					} catch (IOException ex) {
						Logger.getLogger(EventFileTransferTest.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		serverThread.setName("serverThread");
		serverThread.start();
		serverThread.join();
	}
	
	private static final int port = 27070;
	private Object monitor = new Object();

	private void handleEventObject(Socket socket, ObjectInputStream inputStream, ObjectOutputStream outputStream) {
		synchronized (monitor) {
			while (true) {
				try {
					monitor.wait();
				} catch (Exception ex) {
					TransferObject outputObject = new EventObject();
					try {
						byte[] buffer = new byte[16];
						outputStream.write(buffer);
						outputStream.writeObject(outputObject);
						outputStream.flush();
					} catch (Exception e) {
						break;
					} finally {
						try {
							if (!socket.isClosed()) {
								socket.close();
							}
						} catch (Exception ee) {
							// Ignore it!
						}
					}
					break;
				}
			}
		}
	}

	private void wait10Seconds() {
		try {
			Thread.sleep(10 * 1000);
		} catch (InterruptedException ex) {
			Logger.getLogger(EventClusterService.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private Thread th(Consumer<Boolean> runnable, String name) {
		Thread thread = new Thread(() -> {
			runnable.accept(true);
			waitForewer();
		});
		thread.setName(name);
		thread.start();
		return thread;
	}

	private void waitForewer() {
		try {
			Thread.sleep(Integer.MAX_VALUE);
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
	}
}
