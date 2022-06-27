package com.servermanager.services;

import com.utils.SshUtils;
import java.nio.file.Path;

public class UploadSshService extends AbstractService {

	private final String user;
	private final String password;

	public UploadSshService(String host, String user, String password, int port) {
		super(host, port);
		this.user = user;
		this.password = password;
	}

	public void upload(Path to, Path from) throws Exception {
		new SshUtils(host, user, password, port).uploadFiles(from, to);
	}
}
