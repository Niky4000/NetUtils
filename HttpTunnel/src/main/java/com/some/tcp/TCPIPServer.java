/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.some.tcp;

import java.io.IOException;
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
			while (true) {
				try (Socket clientSocket = serverSocket.accept()) {
					InetAddress inetAddress = clientSocket.getInetAddress();
					String hostIp = inetAddress.getHostAddress();
					System.out.println(hostIp);
					try (OutputStream outputStream = clientSocket.getOutputStream()) {
						outputStream.write(hostIp.getBytes());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
