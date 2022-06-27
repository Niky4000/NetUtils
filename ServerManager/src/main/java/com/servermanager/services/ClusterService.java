package com.servermanager.services;

import com.servermanager.services.bean.ClusterListFilesBean;
import com.utils.IgniteUtils;
import com.utils.MapBuilder;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import org.apache.ignite.Ignite;

public class ClusterService extends AbstractService {

	private Ignite ignite;
	private Collection<String> hostList;
	private String instanceName;
	private int initialLocalPort;
	private int endPort;
	private int localPort;
	private File home;

	public ClusterService(String host, int port) {
		super(host, port);
	}

	public ClusterService(Collection<String> hostList, String instanceName, int initialLocalPort, int endPort, int localPort, File home) {
		super(null, 0);
		this.hostList = hostList;
		this.instanceName = instanceName;
		this.initialLocalPort = initialLocalPort;
		this.endPort = endPort;
		this.localPort = localPort;
		this.home = home;
	}

	public void startCluster() {
		this.ignite = IgniteUtils.createServerInstance(new ArrayList<>(hostList), instanceName, initialLocalPort, endPort, localPort);
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
		Optional<ClusterListFilesBean> clusterListFilesBean = Optional.ofNullable(new ClientService(host, port).sendMessage(Arrays.asList(new ClusterListFilesBean<>()).iterator())).map(obj -> (ClusterListFilesBean) obj.get(0));
		clusterListFilesBean.ifPresent(bean -> {
			Map<String, List<File>> listFiles = bean.getListFiles();
			for (Entry<String, List<File>> entry : listFiles.entrySet()) {
				System.out.println("Host: " + entry.getKey());
				for (File file : entry.getValue()) {
					System.out.println("File: " + file.getAbsolutePath());
				}
			}
		});
	}
}
