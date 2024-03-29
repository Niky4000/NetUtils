package com.servermanager.services;

import com.servermanager.StartServerManager;
import static com.servermanager.caches.CacheNames.EVENTS;
import com.servermanager.services.bean.ClusterListFilesBean;
import com.servermanager.services.events.ClipboardEvent;
import com.servermanager.services.events.Event;
import com.servermanager.services.events.FileDeleted;
import com.servermanager.services.events.FileEventKey;
import com.servermanager.services.events.FileUploaded;
import com.utils.CacheUtils;
import com.utils.IgniteUtils;
import static com.utils.Logger.println;
import com.utils.MapBuilder;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.configuration.CacheConfiguration;

public class FilesClusterService extends AbstractService {

	public static final int EVENT_TIME_TO_LIVE = 2;
	private Ignite ignite;
	private Collection<String> hostList;
	private String instanceName;
	private int initialLocalPort;
	private int endPort;
	private int localPort;
	private File home;
	private Integer clientPort;
	private Integer clientPortRange;
	private final StartServerManager startServerManager;

	public FilesClusterService(String host, int port, StartServerManager startServerManager) {
		super(host, port);
		this.startServerManager = startServerManager;
	}

	public FilesClusterService(Collection<String> hostList, String instanceName, int initialLocalPort, int endPort, int localPort, File home, Integer clientPort, Integer clientPortRange, StartServerManager startServerManager) {
		super(null, 0);
		this.hostList = hostList;
		this.instanceName = instanceName;
		this.initialLocalPort = initialLocalPort;
		this.endPort = endPort;
		this.localPort = localPort;
		this.home = home;
		this.clientPort = clientPort;
		this.clientPortRange = clientPortRange;
		this.startServerManager = startServerManager;
	}

	public void startCluster() {
		if (!home.exists()) {
			home.mkdirs();
		}
		this.ignite = IgniteUtils.createServerInstance(new ArrayList<>(hostList), instanceName, initialLocalPort, endPort, localPort, clientPort, clientPortRange);
		CacheUtils.destroyCacheIfItExists(ignite, EVENTS.value());
		CacheConfiguration<FileEventKey, Event> cacheConfiguration = new CacheConfiguration<>();
		cacheConfiguration.setName(EVENTS.value());
		cacheConfiguration.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.MINUTES, EVENT_TIME_TO_LIVE)));
		cacheConfiguration.setEagerTtl(true);
		cacheConfiguration.setOnheapCacheEnabled(true);
		ignite.getOrCreateCache(cacheConfiguration);
	}

	public Map<String, List<File>> listFiles() {
		Collection<Map<String, List<File>>> listOfMaps = ignite.compute().broadcast(() -> {
			String nodeName = ignite.cluster().localNode().id().toString();
			List<File> files = Arrays.asList(home.listFiles());
			Collections.sort(files);
			Map<String, List<File>> map = MapBuilder.<String, List<File>>builder().put(nodeName, files).build();
			return map;
		});
		Map<String, List<File>> map = listOfMaps.stream().reduce(new HashMap<>(), (map1, map2) -> {
			map1.putAll(map2);
			return map1;
		});
		return new TreeMap<>(map);
	}

	public void listFilesClient() throws IOException, ClassNotFoundException {
		Optional<ClusterListFilesBean> clusterListFilesBean = Optional.ofNullable(new ClientService(host, port, startServerManager).sendMessage(Arrays.asList(new ClusterListFilesBean<>(startServerManager)).iterator())).map(obj -> (ClusterListFilesBean) obj.get(0));
		clusterListFilesBean.ifPresent(bean -> {
			Map<String, List<File>> listFiles = bean.getListFiles();
			for (Entry<String, List<File>> entry : listFiles.entrySet()) {
				println("Host: " + entry.getKey());
				for (File file : entry.getValue()) {
					println("File: " + file.getAbsolutePath());
				}
			}
		});
	}

	public void fileUploadedEvent(File file, Date eventDate) {
//		ignite.<FileEventKey, Event>cache(EVENTS.value()).put(new FileEventKey(file.getName()), new FileUploaded(file));
		IgniteCache<FileEventKey, Event> cache = ignite.<FileEventKey, Event>cache(EVENTS.value());
		cache.remove(new FileEventKey(file.getName()));
		cache.put(new FileEventKey(file.getName()), new FileUploaded(file, eventDate));
		startServerManager.getServerListerner().sendInterruptionsToTheRemoteEventListerners();
	}

	public void fileDeletedEvent(File file, Date eventDate) {
//		ignite.<FileEventKey, Event>cache(EVENTS.value()).put(new FileEventKey(file.getName()), new FileDeleted(file));
		IgniteCache<FileEventKey, Event> cache = ignite.<FileEventKey, Event>cache(EVENTS.value());
		cache.remove(new FileEventKey(file.getName()));
		cache.put(new FileEventKey(file.getName()), new FileDeleted(file, eventDate));
		startServerManager.getServerListerner().sendInterruptionsToTheRemoteEventListerners();
	}

	public void clipboardChanged(ClipboardEvent clipboardEvent) {
		FileEventKey fileEventKey = new FileEventKey(clipboardEvent.getClipboardData());
		IgniteCache<FileEventKey, Event> cache = ignite.<FileEventKey, Event>cache(EVENTS.value());
		cache.remove(fileEventKey);
		cache.put(fileEventKey, clipboardEvent);
		startServerManager.getServerListerner().sendInterruptionsToTheRemoteEventListerners();
	}

	public File getHome() {
		return home;
	}
}
