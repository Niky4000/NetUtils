/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.some.tcp;

import static com.httptunneling.TunnelStart.addOpenedPort;
import static com.httptunneling.TunnelStart.isEverythingInterrupted;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author me
 */
public class TCPIPServer {

	public void init(int sourcePort) {
		try {
			ServerSocket serverSocket = new ServerSocket(sourcePort);
			addOpenedPort(sourcePort, serverSocket);
			while (!isEverythingInterrupted()) {
				try (Socket clientSocket = serverSocket.accept()) {
					InetAddress inetAddress = clientSocket.getInetAddress();
					String hostIp = inetAddress.getHostAddress();
					System.out.println(hostIp);
					try (OutputStream outputStream = clientSocket.getOutputStream()) {
						outputStream.write(hostIp.getBytes());
					}
				} catch (Exception e) {
					continue;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
