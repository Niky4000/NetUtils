package com.guichaguri.minimalftp.handler.bean;

import java.net.ServerSocket;
import java.util.Date;

public class ServerSocketBean {

	private final int port;
	private final Date date;
	private final ServerSocket serverSocket;

	public ServerSocketBean(int port, Date date, ServerSocket serverSocket) {
		this.port = port;
		this.date = date;
		this.serverSocket = serverSocket;
	}

	public int getPort() {
		return port;
	}

	public Date getDate() {
		return date;
	}

	public ServerSocket getServerSocket() {
		return serverSocket;
	}
}
