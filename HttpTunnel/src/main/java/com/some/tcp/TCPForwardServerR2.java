package com.some.tcp;

import static com.httptunneling.TunnelStart.addOpenedPort;
import static com.httptunneling.TunnelStart.isEverythingInterrupted;
import com.some.tcp.bean.SocketBean;
import static com.utils.Utils.fireWall;
import java.net.*;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class TCPForwardServerR2 {

	private static final int QUEUE_SIZE = 16;
	private static final int CRITICAL_QUEUE_SIZE = 2;

	public void init(int sourcePort, int destinationPort, Map<Integer, Set<String>> filterMap) {
		SocketBean socketBean = new SocketBean();
		Thread clientListernerThread = new Thread(() -> {
			serverImpl(destinationPort, socketBean, socket -> socketBean.setClientSocket2(socket), filterMap);
		});
		Thread serverListernerThread = new Thread(() -> {
			serverImpl(sourcePort, socketBean, socket -> socketBean.setServerSocket2(socket), filterMap);
		});
		clientListernerThread.setName("clientListernerThread");
		serverListernerThread.setName("serverListernerThread");
		clientListernerThread.start();
		serverListernerThread.start();
		while (!isEverythingInterrupted()) {
			socketBean.waitForSocketsToBeReady();
			Socket clientSocket2 = socketBean.getClientSocket2();
			Socket serverSocket2 = socketBean.getServerSocket2();
			ClientThread clientThread = new ClientThread(clientSocket2, serverSocket2);
			clientThread.setName("clientThread");
			clientThread.start();
		}
	}

	private void serverImpl(final int port, SocketBean socketBean, Consumer<Socket> setter, Map<Integer, Set<String>> filterMap) {
		while (true) {
			ServerSocket serverSocket = null;
			try {
				serverSocket = new ServerSocket(port);
				addOpenedPort(port, serverSocket);
				while (!isEverythingInterrupted()) {
					Socket clientSocket = serverSocket.accept();
					if (fireWall(port, clientSocket, filterMap)) {
						clientSocket.close();
						continue;
					}
					// Turn on keep-alive for both the sockets 
					clientSocket.setKeepAlive(true);
					setter.accept(clientSocket);
					socketBean.notifyIt();
				}
			} catch (Exception e) {
				Logger.log(e);
				if (serverSocket != null) {
					try {
						serverSocket.close();
					} catch (Exception ee) {
						Logger.log(ee);
					}
				}
				if (isEverythingInterrupted()) {
					break;
				}
			}
		}
	}
}
