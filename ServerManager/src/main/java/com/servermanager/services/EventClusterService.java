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
import java.util.Iterator;
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
			ignite.<FileEventKey, Event>cache(EVENTS.value());
			Iterator<Cache.Entry<FileEventKey, Event>> iterator = ignite.<FileEventKey, Event>cache(EVENTS.value()).query(new ScanQuery<FileEventKey, Event>((date, event) -> event instanceof FileEvent)).iterator();
			while (iterator.hasNext()) {
				Cache.Entry<FileEventKey, Event> next = iterator.next();
				FileEventKey key = next.getKey();
				FileEvent fileEvent = (FileEvent) next.getValue();
				if (getHandledEvents().asMap().containsKey(key)) {
					continue;
				}
				if (fileEvent instanceof FileUploaded) {
					try {
						new DownloadService(host, port).download(fileEvent.getFile().toPath(), home.toPath().resolve(fileEvent.getFile().getName()));
						System.out.println("File event: " + fileEvent.getFile().getAbsolutePath() + " was downloaded!");
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else if (fileEvent instanceof FileDeleted) {
					home.toPath().resolve(fileEvent.getFile().getName()).toFile().delete();
					System.out.println("File event: " + fileEvent.getFile().getAbsolutePath() + " was deleted!");
				}
				getHandledEvents().put(key, fileEvent);
			}
			try {
				Thread.sleep(10 * 1000);
			} catch (InterruptedException ex) {
				Logger.getLogger(EventClusterService.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	public com.github.benmanes.caffeine.cache.Cache<FileEventKey, Event> getHandledEvents() {
		return handledEvents;
	}
}
