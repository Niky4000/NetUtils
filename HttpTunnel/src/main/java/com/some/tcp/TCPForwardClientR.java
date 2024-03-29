package com.some.tcp;

import static com.httptunneling.TunnelStart.isEverythingInterrupted;
import static com.httptunneling.utils.NetUtils.handleError;
import java.net.Socket;

/**
 * @author NAnishhenko
 */
public class TCPForwardClientR {

//    public static void main(String[] args) throws IOException {
//        new TCPForwardClientR().init("192.168.192.216", 22888, "172.29.4.26", 22);
//    }
	public void init(String sourceHost, int sourcePort, String destinationHost, int destinationPort) {
		while (!isEverythingInterrupted()) {
			try {
				Socket clientSocket = new Socket(sourceHost, sourcePort);
				Socket mServerSocket = new Socket(destinationHost, destinationPort);
				// Turn on keep-alive for both the sockets 
				mServerSocket.setKeepAlive(true);
				clientSocket.setKeepAlive(true);
				ClientThread clientThread = new ClientThread(clientSocket, mServerSocket);
				clientThread.start();
				clientThread.getSemaphore().acquire();
			} catch (Exception e) {
				if (handleError(e, "RC")) {
					break;
				}
			}
		}
	}
}
