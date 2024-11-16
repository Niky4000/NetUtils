package com.servermanager.services.bean;

import com.servermanager.StartServerManager;
import com.utils.FileUtils;
import java.io.File;

public class GetPathToJarInputObject<T> extends TransferObject<T> {

	private File pathToJar;
	private String md5Sum;

	public GetPathToJarInputObject() {
		super();
	}

	public GetPathToJarInputObject(File pathToJar, String md5Sum) {
		this.pathToJar = pathToJar;
		this.md5Sum = md5Sum;
	}

	public File getPathToJar() {
		return pathToJar;
	}

	public String getMd5Sum() {
		return md5Sum;
	}

	@Override
	public TransferObject apply(TransferObject<T> object, StartServerManager startServerManager) {
		return new GetPathToJarInputObject(FileUtils.getPathToJar(), FileUtils.getMd5Sum(FileUtils.getPathToJar().getAbsoluteFile()));
	}
}
