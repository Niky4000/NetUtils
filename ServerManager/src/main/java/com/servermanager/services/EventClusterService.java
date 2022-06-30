package com.servermanager.services;

import static com.servermanager.caches.CacheNames.EVENTS;
import static com.servermanager.caches.CacheNames.FILES;
import com.servermanager.services.events.Event;
import com.servermanager.services.events.FileDeleted;
import com.servermanager.services.events.FileEvent;
import com.servermanager.services.events.FileUploaded;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.cache.Cache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.client.ClientCache;
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
			ignite.<Date, Event>cache(EVENTS.value());
			Iterator<Cache.Entry<Date, Event>> iterator = ignite.<Date, Event>cache(FILES.value()).query(new ScanQuery<Date, Event>((date, event) -> event instanceof FileEvent)).iterator();
			while (iterator.hasNext()) {
				Cache.Entry<Date, Event> next = iterator.next();
				Date date = next.getKey();
				FileEvent fileEvent = (FileEvent) next.getValue();
				if (fileEvent instanceof FileUploaded) {
					try {
						new DownloadService(host, port).download(fileEvent.getFile().toPath(), home.toPath());
						System.out.println("File event: " + fileEvent.getFile().getAbsolutePath() + " was downloaded!");
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else if (fileEvent instanceof FileDeleted) {
					home.toPath().resolve(fileEvent.getFile().getName()).toFile().delete();
					System.out.println("File event: " + fileEvent.getFile().getAbsolutePath() + " was deleted!");
				}
			}
			try {
				Thread.sleep(10 * 1000);
			} catch (InterruptedException ex) {
				Logger.getLogger(EventClusterService.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
//		IgniteEvents events = ignite.events();
//		events.remoteListen((uuid, event) -> {
//			System.out.println("Event name = " + event.name() + "!");
//			return true;
//		}, event -> true, EventType.EVT_CACHE_OBJECT_PUT);
	}
}
