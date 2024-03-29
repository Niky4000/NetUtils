package com.servermanager;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.lib.ConfigHandler;
import static com.lib.ConfigHandler.getParameter;
import static com.lib.ConfigHandler.getParameters;
import com.servermanager.services.ClipboardService;
import com.servermanager.services.FilesClusterService;
import com.servermanager.services.DeleteService;
import com.servermanager.services.DownloadService;
import com.servermanager.services.EventClusterService;
import static com.servermanager.services.ObservableFileSystemService.createFileSystemListerner;
import static com.servermanager.services.ObservableFileSystemService.initActionsBeforeCreatingTheListerners;
import com.servermanager.services.SelfUpdateService;
import com.servermanager.services.ServerListerner;
import com.servermanager.services.UpdateService;
import com.servermanager.services.UpdateSshService;
import com.servermanager.services.UploadService;
import com.servermanager.services.UploadSshService;
import static com.utils.Logger.println;
import static com.utils.Logger.setLog;
import com.utils.WaitUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class StartServerManager {

	private static final String SAVE_FOLDER_NAME = "dat";
	private static final String CRYPTED_CONFIG = "sys.dat";
	private FilesClusterService clusterService;
	private ServerListerner serverListerner;
	private ConcurrentMap<File, EventClusterService> eventClusterServiceMap = new ConcurrentHashMap<>();
	public static final int EVENT_TIME_TO_LIVE = 30;
	private final com.github.benmanes.caffeine.cache.Cache<String, Date> downloadedFiles = Caffeine.<String, Date>newBuilder().expireAfterWrite(EVENT_TIME_TO_LIVE, TimeUnit.SECONDS).build();

	public static void main(String[] args) {
		try {
			new ConfigHandler(SAVE_FOLDER_NAME, CRYPTED_CONFIG).start(args, (args_, argList2) -> new StartServerManager().argumentsHandler(args_, argList2));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private void argumentsHandler(String[] args, List<List<String>> argList2) {
		try {
			setLog(false);
			new SelfUpdateService().update(args);
			for (int i = 0; i < argList2.size(); i++) {
				List<String> argList = argList2.get(i);
				if (!argList.isEmpty()) {
					if (argList.get(0).equals("START")) {
						Integer port = Integer.valueOf(getParameter("-port", argList));
						serverListerner = new ServerListerner(port, this);
						serverListerner.listen();
					}
					if (argList.get(0).equals("CLUSTER")) {
						Map<String, String> hostMap = getParameters("-host", argList);
						File home = new File(getParameter("-home", argList));
						String instanceName = getParameter("-instanceName", argList);
						Integer initialLocalPort = Integer.valueOf(getParameter("-port", argList));
						Integer endPort = Integer.valueOf(getParameter("-endPort", argList));
						Integer localPort = Integer.valueOf(getParameter("-localPort", argList));
						Integer clientPort = Optional.ofNullable(getParameter("-clientPort", argList)).map(Integer::valueOf).orElse(null);
						Integer clientPortRange = Optional.ofNullable(getParameter("-clientPortRange", argList)).map(Integer::valueOf).orElse(null);
						clusterService = new FilesClusterService(hostMap.values(), instanceName, initialLocalPort, endPort, localPort, home, clientPort, clientPortRange, this);
						clusterService.startCluster();
					} else if (argList.get(0).equals("UPDATE")) {
						String host = getParameter("-host", argList);
						Integer port = Integer.valueOf(getParameter("-port", argList));
						new UpdateService(host, port, this).update(args);
					} else if (argList.get(0).equals("UPLOAD")) {
						String host = getParameter("-host", argList);
						Integer port = Integer.valueOf(getParameter("-port", argList));
						Path to = Paths.get(getParameter("-to", argList));
						Path from = Paths.get(getParameter("-from", argList));
						new UploadService(host, port, this).upload(to, from, new Date());
					} else if (argList.get(0).equals("SELF_UPDATE")) {
						new SelfUpdateService().update(args);
					} else if (argList.get(0).equals("DOWNLOAD")) {
						String host = getParameter("-host", argList);
						Integer port = Integer.valueOf(getParameter("-port", argList));
						Path from = Paths.get(getParameter("-from", argList));
						Path to = Paths.get(getParameter("-to", argList));
						new DownloadService(host, port, this).download(from, to, new Date());
					} else if (argList.get(0).equals("DELETE")) {
						String host = getParameter("-host", argList);
						Integer port = Integer.valueOf(getParameter("-port", argList));
						Path to = Paths.get(getParameter("-to", argList));
						new DeleteService(host, port, this).delete(to, new Date());
					} else if (argList.get(0).equals("UPDATE_SSH")) {
						String host = getParameter("-host", argList);
						String user = getParameter("-user", argList);
						String password = getParameter("-password", argList);
						Integer port = Integer.valueOf(getParameter("-port", argList));
						Path to = Paths.get(getParameter("-to", argList));
						String command = getParameter("-exec", argList);
						new UpdateSshService(host, user, password, port).update(to, command);
					} else if (argList.get(0).equals("UPLOAD_SSH")) {
						String host = getParameter("-host", argList);
						String user = getParameter("-user", argList);
						String password = getParameter("-password", argList);
						Integer port = Integer.valueOf(getParameter("-port", argList));
						Path to = Paths.get(getParameter("-to", argList));
						Path from = Paths.get(getParameter("-from", argList));
						new UploadSshService(host, user, password, port).upload(to, from);
					} else if (argList.get(0).equals("C_LIST_FILES")) {
						String host = getParameter("-host", argList);
						Integer port = Integer.valueOf(getParameter("-port", argList));
						new FilesClusterService(host, port, this).listFilesClient();
					} else if (argList.get(0).equals("WATCH")) {
						File dir = new File(getParameter("-dir", argList));
						String host = getParameter("-host", argList);
						Integer port = Integer.valueOf(getParameter("-port", argList));
						Path to = Paths.get(getParameter("-to", argList));
						initActionsBeforeCreatingTheListerners(host, port, to, dir, this);
						createFileSystemListerner(host, port, to, dir, this);
					} else if (argList.get(0).equals("CLIPBOARD")) {
						String host = getParameter("-host", argList);
						Integer port = Integer.valueOf(getParameter("-port", argList));
						ClipboardService clipboardService = new ClipboardService(host, port, this);
						clipboardService.createClipboardThread();
					} else if (argList.get(0).equals("EVENT")) {
						String host = getParameter("-host", argList);
						Integer port = Integer.valueOf(getParameter("-port", argList));
						File home = new File(getParameter("-dir", argList));
						String instanceName = getParameter("-instanceName", argList);
						Integer clientPort = Optional.ofNullable(getParameter("-clientPort", argList)).map(Integer::valueOf).orElse(null);
						Integer clientPortRange = Optional.ofNullable(getParameter("-portRange", argList)).map(Integer::valueOf).orElse(null);
						EventClusterService eventService = new EventClusterService(host, port, instanceName, clientPort, clientPortRange, home, this);
						getEventClusterServiceMap().put(home, eventService);
						eventService.startCluster();
						eventService.listenToFileEvents();
					} else if (argList.get(0).equals("LOG")) {
						setLog(true);
					} else if (argList.get(0).equals("DEBUG")) {
						debug();
						WaitUtils.waitSomeTime(2000);
					}
				} else {
					debug();
				}
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private static void debug() throws IOException, InterruptedException {
		println("Wrong arguments!");
		WaitUtils.waitSomeTime(20);
		createTestFile();
		println("Wrong arguments!");
	}

	private static void createTestFile() throws IOException {
		File file = new File("created");
		if (!file.exists()) {
			file.createNewFile();
		}
		Files.write(file.toPath(), (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n").getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
	}

	public FilesClusterService getClusterService() {
		return clusterService;
	}

	public ConcurrentMap<File, EventClusterService> getEventClusterServiceMap() {
		return eventClusterServiceMap;
	}

	public ServerListerner getServerListerner() {
		return serverListerner;
	}

	public com.github.benmanes.caffeine.cache.Cache<String, Date> getDownloadedFiles() {
		return downloadedFiles;
	}
}
