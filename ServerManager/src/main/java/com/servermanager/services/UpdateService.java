package com.servermanager.services;

import com.servermanager.services.bean.GetPathToJarInputObject;
import com.utils.FileUtils;

import static com.utils.FileUtils.launchSelf;

import com.utils.WaitUtils;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;

public class UpdateService extends AbstractService {

	private static final String dot = ".";
	public static final String UPDATE = "_update";
	private static final int TIME_TO_WAIT = 60;

	public UpdateService(String host, int port) {
		super(host, port);
	}

	public void update(String[] args) {
		Thread updateThread = new Thread(() -> {
			while (true) {
				try {
					WaitUtils.waitSomeTime(TIME_TO_WAIT);
					GetPathToJarInputObject getPathToJarInputObject = ((GetPathToJarInputObject) new ClientService(host, port).sendMessage(Arrays.asList(new GetPathToJarInputObject<>()).iterator()).get(0));
					File from = getPathToJarInputObject.getPathToJar();
					Path pathToJar = FileUtils.getPathToJar().toPath();
					String md5Sum = getPathToJarInputObject.getMd5Sum();
					String selfMd5Sum = FileUtils.getMd5Sum(pathToJar.toFile());
					if (!md5Sum.equals(selfMd5Sum)) {
						Path to = pathToJar.getParent().resolve(addUpdateMarkToFileName(from.getName()));
						new DownloadService(host, port).download(from.toPath(), to, new Date());
						String downloadedMd5Sum = FileUtils.getMd5Sum(to.toFile());
						launchSelf(args, to);
						System.exit(0);
					}
				} catch (Exception e) {
					continue;
				}
			}
		});
		updateThread.setName("UpdateThread");
		updateThread.start();
	}

	private String addUpdateMarkToFileName(String fileName) {
		return fileName.substring(0, fileName.lastIndexOf(dot)) + UPDATE + fileName.substring(fileName.lastIndexOf(dot));
	}
}
