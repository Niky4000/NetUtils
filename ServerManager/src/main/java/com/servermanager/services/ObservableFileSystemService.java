package com.servermanager.services;

import static com.servermanager.StartServerManager.getEventClusterServiceMap;

import com.servermanager.observable.threads.FileSystemObserverThread;
import com.servermanager.observable.threads.WatchThread;
import com.servermanager.services.events.FileDeleted;
import com.servermanager.services.events.FileEventKey;
import com.servermanager.services.events.FileUploaded;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ObservableFileSystemService {

	private final File home;
	private static final String OBSERVABLE = "observable_";
	private static AtomicInteger fileSystemObserverThreadCounter = new AtomicInteger(0);

	public ObservableFileSystemService(File home) {
		this.home = home;
		List<File> observableDirs = Arrays.stream(this.home.listFiles()).filter(file -> file.getName().startsWith(OBSERVABLE)).collect(Collectors.toList());
	}

	public static void initActionsBeforeCreatingTheListerners(String host, Integer port, Path to, File dir) throws IOException, ClassNotFoundException, Exception {
		List<File> remoteFileList = new FileService(host, port).getFileList(to.toFile());
		List<File> localFileList = Arrays.asList(dir.listFiles());
		Map<String, File> remoteFileMap = getFileMap(remoteFileList);
		Map<String, File> localFileMap = getFileMap(localFileList);
		Map<String, File> remoteFileMapWithoutLocalFiles = getFilesThatDontExistOnOtherSide(remoteFileMap, localFileMap);
		Map<String, File> localFileMapWithoutRemoteFiles = getFilesThatDontExistOnOtherSide(localFileMap, remoteFileMap);
		if (!remoteFileMapWithoutLocalFiles.isEmpty()) {
			DownloadService downloadService = new DownloadService(host, port);
			for (File file : remoteFileMapWithoutLocalFiles.values()) {
				downloadService.download(file.toPath(), dir.toPath().resolve(file.getName()), new Date());
			}
		}
		if (!localFileMapWithoutRemoteFiles.isEmpty()) {
			UploadService uploadService = new UploadService(host, port);
			for (File file : localFileMapWithoutRemoteFiles.values()) {
				uploadService.upload(to.resolve(file.getName()), file.toPath(), new Date());
			}
		}
	}

	private static Map<String, File> getFilesThatDontExistOnOtherSide(Map<String, File> map1, Map<String, File> map2) {
		HashMap<String, File> map1_ = new HashMap<>(map1);
		HashSet<String> set = new HashSet<>(map1.keySet());
		set.retainAll(map2.keySet());
		set.forEach(name -> map1_.remove(name));
		return map1_;
	}

	private static Map<String, File> getFileMap(List<File> fileList) {
		return fileList.stream().collect(Collectors.toMap(File::getName, file -> file));
	}

	public static void createFileSystemListerner(String host, Integer port, Path to, File dir) {
		AtomicInteger modificationCounter = new AtomicInteger(0);
		int threadIndex = fileSystemObserverThreadCounter.incrementAndGet();
		WatchThread watchThread = new WatchThread(threadIndex, modifiedFileSet -> {
			for (File file : modifiedFileSet) {
				try {
					if (Optional.ofNullable(getEventClusterServiceMap().get(dir)).map(eventService -> eventService.getHandledEvents()).map(cache -> cache.asMap().containsKey(new FileEventKey(file.getName()))).orElse(false)) {
						continue;
					}
					Date eventDate = new Date();
					if (file.exists()) {
						getEventClusterServiceMap().get(dir).getHandledEvents().put(new FileEventKey(file.getName()), new FileUploaded(file, eventDate));
						new UploadService(host, port).upload(to.resolve(file.getName()), file.toPath(), eventDate);
						System.out.println("File " + file.getAbsolutePath() + " was sent!");
					} else {
						getEventClusterServiceMap().get(dir).getHandledEvents().put(new FileEventKey(file.getName()), new FileDeleted(file, eventDate));
						new DeleteService(host, port).delete(to.resolve(file.getName()), eventDate);
						System.out.println("File " + file.getAbsolutePath() + " was deleted!");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, modificationCounter);
		watchThread.start();
		FileSystemObserverThread fileSystemObserverThread = new FileSystemObserverThread(threadIndex, dir, watchThread, modificationCounter);
		fileSystemObserverThread.start();
	}
}
