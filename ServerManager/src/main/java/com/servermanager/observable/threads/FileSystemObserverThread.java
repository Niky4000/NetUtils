package com.servermanager.observable.threads;

import com.servermanager.StartServerManager;
import static com.utils.Logger.println;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class FileSystemObserverThread extends Thread implements InterruptableThread {

	private final File dir;
	private final WatchThread watchThread;
	private final AtomicInteger modificationCounter;
	private volatile boolean interruptedExternally = false;
	private static final String s = FileSystems.getDefault().getSeparator();
	private static final String STREAM_SIGN = ".goutputstream";
	private final StartServerManager startServerManager;
	public static final Date nullDate = new Date(0L);

	public FileSystemObserverThread(int i, File dir, WatchThread watchThread, AtomicInteger modificationCounter, StartServerManager startServerManager) {
		super("FileSystemObserverThread_" + i);
		this.dir = dir;
		this.watchThread = watchThread;
		this.modificationCounter = modificationCounter;
		this.startServerManager = startServerManager;
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
						println("Event kind:" + event.kind() + ". File affected: " + event.context() + ".");
						if (event.context() instanceof Path) {
							File file = ((Path) event.context()).toFile();
							if (file.getName().startsWith(STREAM_SIGN)) {
								continue;
							}
							File modifiedFile = new File(dir.getAbsolutePath() + s + file.getName());
							AtomicReference<Date> lastModifiedTime = new AtomicReference<>();
							if (modifiedFile.exists()) {
								BasicFileAttributes attributes = Files.readAttributes(modifiedFile.toPath(), BasicFileAttributes.class);
								lastModifiedTime.set(new Date(attributes.lastModifiedTime().toMillis()));
							}
							AtomicBoolean conditon = new AtomicBoolean(false);
							startServerManager.getDownloadedFiles().asMap().computeIfPresent(modifiedFile.getAbsolutePath(), (path, date) -> {
								if (lastModifiedTime.get() != null && !lastModifiedTime.get().after(date)) {
									conditon.set(true);
								}
								return date;
							});
							if (conditon.get()) {
								println(modifiedFile.getAbsolutePath() + " is downloading at the moment!");
								continue;
							}
							watchThread.addModifiedFileSet(modifiedFile);
							println("file = " + modifiedFile.getAbsolutePath() + " size = " + (modifiedFile.exists() ? modifiedFile.length() : 0) + "!");
							modificationCounter.incrementAndGet();
							watchThread.interrupt();
						}
						if (event.kind().equals(ENTRY_MODIFY)) {
							println("Modification!");
						} else if (event.kind().equals(ENTRY_DELETE)) {
							println("Deletion!");
						}
					}
					key.reset();
				} catch (InterruptedException ie) {
					if (interruptedExternally) {
						break;
					}
					println("setObserver InterruptedException!", ie);
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
