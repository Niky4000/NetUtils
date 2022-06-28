package com.servermanager.observable.threads;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSystemObserverThread extends Thread implements InterruptableThread {

	private final static Logger logger = LoggerFactory.getLogger(FileSystemObserverThread.class);

	private final File dir;
	private final WatchThread watchThread;
	private final AtomicInteger modificationCounter;
	private volatile boolean interruptedExternally = false;
	private static final String s = FileSystems.getDefault().getSeparator();
	private static final String STREAM_SIGN = ".goutputstream";

	public FileSystemObserverThread(int i, File dir, WatchThread watchThread, AtomicInteger modificationCounter) {
		super("FileSystemObserverThread_" + i);
		this.dir = dir;
		this.watchThread = watchThread;
		this.modificationCounter = modificationCounter;
	}

	@Override
	public void run() {
		try {
			WatchService watchService = FileSystems.getDefault().newWatchService();
			dir.toPath().register(watchService, ENTRY_DELETE, ENTRY_MODIFY, ENTRY_CREATE, OVERFLOW);
			while (true) {
				WatchKey key = null;
				try {
					key = watchService.take();
					for (WatchEvent<?> event : key.pollEvents()) {
						logger.debug("Event kind:" + event.kind() + ". File affected: " + event.context() + ".");
						if (event.context() instanceof Path) {
							File file = ((Path) event.context()).toFile();
							if (file.getName().startsWith(STREAM_SIGN)) {
								continue;
							}
							File modifiedFile = new File(dir.getAbsolutePath() + s + file.getName());
							watchThread.addModifiedFileSet(modifiedFile);
							logger.debug("file = " + modifiedFile.getAbsolutePath() + " size = " + (modifiedFile.exists() ? modifiedFile.length() : 0) + "!");
							System.out.println("file = " + modifiedFile.getAbsolutePath() + " size = " + (modifiedFile.exists() ? modifiedFile.length() : 0) + "!");
							modificationCounter.incrementAndGet();
							watchThread.interrupt();
						}
						if (event.kind().equals(ENTRY_MODIFY)) {
							logger.debug("Modification!");
						} else if (event.kind().equals(ENTRY_DELETE)) {
							logger.debug("Deletion!");
						}
					}
					key.reset();
				} catch (InterruptedException ie) {
					if (interruptedExternally) {
						break;
					}
					logger.error("setObserver InterruptedException!", ie);
				}
			}
		} catch (IOException ex) {
			throw new RuntimeException("setObserver Fatal Exception!", ex);
		}
	}

	@Override
	public void setInterruptFlag() {
		interruptedExternally = true;
	}
}
