package com.servermanager.services;

import com.github.benmanes.caffeine.cache.Caffeine;
import static com.servermanager.caches.CacheNames.EVENTS;
import static com.servermanager.services.FilesClusterService.EVENT_TIME_TO_LIVE;
import com.servermanager.services.events.Event;
import com.servermanager.services.events.FileDeleted;
import com.servermanager.services.events.FileEvent;
import com.servermanager.services.events.FileEventKey;
import com.servermanager.services.events.FileUploaded;
import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.cache.Cache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;

public class EventClusterService {

	private IgniteClient ignite;
	private final String host;
	private final Integer port;
	private final String instanceName;
	private final Integer clientPort;
	private final Integer clientPortRange;
	private final File home;
	private final com.github.benmanes.caffeine.cache.Cache<FileEventKey, Event> handledEvents = Caffeine.<FileEventKey, Event>newBuilder().expireAfterWrite(EVENT_TIME_TO_LIVE, TimeUnit.MINUTES).build();

	public EventClusterService(String host, Integer port, String instanceName, Integer clientPort, Integer clientPortRange, File home) {
		this.host = host;
		this.port = port;
		this.instanceName = instanceName;
		this.clientPort = clientPort;
		this.clientPortRange = clientPortRange;
		this.home = home;
	}

	public void startCluster() {
		ClientConfiguration cfg = new ClientConfiguration().setAddresses(host + ":" + clientPort + ".." + (clientPort + clientPortRange));
		ignite = Ignition.startClient(cfg);
	}

	public void listenToFileEvents() {
		while (true) {
			try {
				int eventCounter = 0;
				Iterator<Cache.Entry<FileEventKey, Event>> iterator = ignite.<FileEventKey, Event>cache(EVENTS.value()).query(new ScanQuery<FileEventKey, Event>((date, event) -> event instanceof FileEvent)).iterator();
//				Iterator<Cache.Entry<FileEventKey, Event>> iterator = ignite.<FileEventKey, Event>cache(EVENTS.value()).query(new ScanQuery<FileEventKey, Event>()).iterator();
				while (iterator.hasNext()) {
					eventCounter++;
					Cache.Entry<FileEventKey, Event> next = iterator.next();
					FileEventKey key = next.getKey();
					FileEvent fileEvent = (FileEvent) next.getValue();
					if (Optional.ofNullable(getHandledEvents().asMap().get(key)).map(Event::getDate).map(date -> {
						Date fileEventDate = fileEvent.getDate();
						boolean b = date.equals(fileEventDate);
						if (!b) {
							System.out.println("File: " + key.getName() + " eventDate: " + fileEventDate + "!");
						}
						return b;
					}).orElse(false)) {
						continue;
					}
					if (fileEvent instanceof FileUploaded) {
						try {
							getHandledEvents().put(key, fileEvent);
							new DownloadService(host, port).download(fileEvent.getFile().toPath(), home.toPath().resolve(fileEvent.getFile().getName()));
							System.out.println("File event: " + fileEvent.getFile().getAbsolutePath() + " was downloaded!");
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else if (fileEvent instanceof FileDeleted) {
						getHandledEvents().put(key, fileEvent);
						home.toPath().resolve(fileEvent.getFile().getName()).toFile().delete();
						System.out.println("File event: " + fileEvent.getFile().getAbsolutePath() + " was deleted!");
					}
				}
				if (eventCounter == 0) {
					System.out.println("No events happened!");
				}
				eventCounter = 0;
				try {
					Thread.sleep(10 * 1000);
				} catch (InterruptedException ex) {
					Logger.getLogger(EventClusterService.class.getName()).log(Level.SEVERE, null, ex);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public com.github.benmanes.caffeine.cache.Cache<FileEventKey, Event> getHandledEvents() {
		return handledEvents;
	}
}
