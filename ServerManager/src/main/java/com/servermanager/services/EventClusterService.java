package com.servermanager.services;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.servermanager.StartServerManager;
import static com.servermanager.caches.CacheNames.EVENTS;
import static com.servermanager.observable.threads.FileSystemObserverThread.nullDate;
import static com.servermanager.services.FilesClusterService.EVENT_TIME_TO_LIVE;
import com.servermanager.services.bean.EventObject;
import com.servermanager.services.events.ClipboardEvent;
import com.servermanager.services.events.Event;
import com.servermanager.services.events.FileDeleted;
import com.servermanager.services.events.FileEvent;
import com.servermanager.services.events.FileEventKey;
import com.servermanager.services.events.FileUploaded;
import com.utils.ClipboardUtils;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Date;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.cache.Cache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;
import static com.utils.Logger.println;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class EventClusterService {

	private IgniteClient ignite;
	private final String host;
	private final Integer port;
	private final String instanceName;
	private final Integer clientPort;
	private final Integer clientPortRange;
	private final File home;
	private final StartServerManager startServerManager;
	private final com.github.benmanes.caffeine.cache.Cache<FileEventKey, Event> handledEvents = Caffeine.<FileEventKey, Event>newBuilder().expireAfterWrite(EVENT_TIME_TO_LIVE + EVENT_TIME_TO_LIVE, TimeUnit.MINUTES).build();

	public EventClusterService(String host, Integer port, String instanceName, Integer clientPort, Integer clientPortRange, File home, StartServerManager startServerManager) {
		this.host = host;
		this.port = port;
		this.instanceName = instanceName;
		this.clientPort = clientPort;
		this.clientPortRange = clientPortRange;
		this.home = home;
		this.startServerManager = startServerManager;
	}

	public void startCluster() {
		ClientConfiguration cfg = new ClientConfiguration().setAddresses(host + ":" + clientPort + ".." + (clientPort + clientPortRange));
		ignite = Ignition.startClient(cfg);
	}

	public void listenToFileEvents() {
		createEventThread(Thread.currentThread());
		while (true) {
			try {
				int eventCounter = 0;
				println(Thread.currentThread().getName() + " is going to check server events!");
				Iterator<Cache.Entry<FileEventKey, Event>> iterator = ignite.<FileEventKey, Event>cache(EVENTS.value()).query(new ScanQuery<FileEventKey, Event>((date, event) -> event instanceof Event)).iterator();
//				Iterator<Cache.Entry<FileEventKey, Event>> iterator = ignite.<FileEventKey, Event>cache(EVENTS.value()).query(new ScanQuery<FileEventKey, Event>()).iterator();
				while (iterator.hasNext()) {
					eventCounter++;
					Cache.Entry<FileEventKey, Event> next = iterator.next();
					FileEventKey key = next.getKey();
					Event event = (Event) next.getValue();
					if (Optional.ofNullable(getHandledEvents().asMap().get(key)).map(Event::getDate).map(date -> {
						Date fileEventDate = event.getDate();
						boolean b = date.equals(fileEventDate);
						if (!b) {
							println("File: " + key.getName() + " date: " + date + " eventDate: " + fileEventDate + "!");
						}
						return b;
					}).orElse(false)) {
						continue;
					}
					if (event instanceof FileUploaded) {
						try {
							Event oldEvent = getHandledEvents().getIfPresent(key);
							if (oldEvent == null || oldEvent.getDate().before(event.getDate()) || oldEvent.getDate().equals(event.getDate())) {
								Date now = event.getDate();
								getHandledEvents().put(key, new Event(now));
								Path to = home.toPath().resolve(((FileEvent) event).getFile().getName());
								startServerManager.getDownloadedFiles().put(to.toFile().getAbsolutePath(), nullDate);
								new DownloadService(host, port, startServerManager).download(((FileEvent) event).getFile().toPath(), to, now);
								if (to.toFile().exists()) {
									BasicFileAttributes attributes = Files.readAttributes(to, BasicFileAttributes.class);
									Date lastModifiedTime = new Date(attributes.lastModifiedTime().toMillis());
									startServerManager.getDownloadedFiles().put(to.toFile().getAbsolutePath(), lastModifiedTime);
								}
								println("File event: " + home.toPath().resolve(((FileEvent) event).getFile().getName()).toFile().getAbsolutePath() + " was downloaded!");
							}
						} catch (Exception e) {
							println(e);
						}
					} else if (event instanceof FileDeleted) {
						Event oldEvent = getHandledEvents().getIfPresent(key);
						File fileToDelete = home.toPath().resolve(((FileEvent) event).getFile().getName()).toFile();
						if (fileToDelete.exists() && (oldEvent == null || oldEvent.getDate().before(event.getDate()) || oldEvent.getDate().equals(event.getDate()))) {
							getHandledEvents().put(key, event);
							fileToDelete.delete();
							println("File event: " + fileToDelete.getAbsolutePath() + " was deleted!");
						}
					} else if (event instanceof ClipboardEvent) {
						Event oldEvent = getHandledEvents().getIfPresent(key);
						if (oldEvent == null || oldEvent.getDate().before(event.getDate()) || oldEvent.getDate().equals(event.getDate())) {
							getHandledEvents().put(key, event);
							ClipboardUtils.setClipboard(((ClipboardEvent) event).getClipboardData());
							println("ClipboardEvent event: " + ((ClipboardEvent) event).getClipboardData() + " was handled!");
						}
					}
				}
//				if (eventCounter == 0) {
//					println("No events happened!");
//				}
				eventCounter = 0;
				wait10Seconds();
			} catch (Exception e) {
				println(e);
				wait10Seconds();
			}
		}
	}

	private void createEventThread(Thread threadToInterrupt) {
		Thread eventThread = new Thread(() -> {
			while (true) {
				try (Socket socket = new Socket(host, port);
						ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
						ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());) {
//					socket.setSoTimeout(10000); // Debug!
					outputStream.writeObject(new EventObject());
					outputStream.flush();
					Object readObject = inputStream.readObject(); // It will be blocked here!
					println(readObject);
				} catch (Exception ex) {
//					ex.printStackTrace();
					println("eventThread exception!", ex);
					wait10Seconds();
				} finally {
					threadToInterrupt.interrupt();
				}
			}
		});
		eventThread.setName("EventThread");
		eventThread.start();
	}

	private void wait10Seconds() {
		try {
			Thread.sleep(10 * 1000);
		} catch (InterruptedException ex) {
//			Logger.getLogger(EventClusterService.class.getName()).log(Level.SEVERE, null, ex);
			println(Thread.currentThread().getName() + " was interrupted!");
		}
	}

	private void wait200Seconds() {
		try {
			Thread.sleep(200 * 1000);
		} catch (InterruptedException ex) {
//			Logger.getLogger(EventClusterService.class.getName()).log(Level.SEVERE, null, ex);
			println(Thread.currentThread().getName() + " was interrupted!");
		}
	}

	public com.github.benmanes.caffeine.cache.Cache<FileEventKey, Event> getHandledEvents() {
		return handledEvents;
	}
}
