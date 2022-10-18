package com.httptunneling.management;

import static com.httptunneling.TunnelStart.addOpenedPort;
import static com.httptunneling.TunnelStart.interruptEverything;
import static com.httptunneling.utils.NetUtils.readInputStream;
import com.lib.ConfigHandler;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class ManagementServer {

	public void init(int sourcePort) {
		try {
			ServerSocket serverSocket = new ServerSocket(sourcePort);
			addOpenedPort(null, serverSocket);
			while (true) {
				Socket clientSocket = serverSocket.accept();
				try {
					ByteArrayOutputStream byteArrayOutputStream = readInputStream(() -> clientSocket);
					String string = new String(byteArrayOutputStream.toByteArray());
					String[] args = string.trim().split(" ");
					List<List<String>> argList2 = ConfigHandler.getArgList(args);
					interruptEverything(args, argList2, Thread.currentThread());
					break;
				} catch (Exception e) {
					continue;
				}
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
}
