package com.servermanager.services;

public abstract class AbstractService {

	final String host;
	final int port;

	public AbstractService(String host, int port) {
		this.host = host;
		this.port = port;
	}
}
