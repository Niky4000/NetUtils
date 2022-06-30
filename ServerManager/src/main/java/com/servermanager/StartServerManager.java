package com.servermanager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lib.ConfigHandler;
import static com.lib.ConfigHandler.getParameter;
import static com.lib.ConfigHandler.getParameters;
import com.servermanager.services.FilesClusterService;
import com.servermanager.services.DeleteService;
import com.servermanager.services.DownloadService;
import com.servermanager.services.EventClusterService;
import static com.servermanager.services.FilesClusterService.EVENT_TIME_TO_LIVE;
import com.servermanager.services.SelfUpdateService;
import com.servermanager.services.ServerListerner;
import com.servermanager.services.UpdateService;
import com.servermanager.services.UpdateSshService;
import com.servermanager.services.UploadService;
import com.servermanager.services.UploadSshService;
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
import static com.servermanager.services.ObservableFileSystemService.createFileSystemListerner;
import static com.servermanager.services.ObservableFileSystemService.initActionsBeforeCreatingTheListerners;
import com.servermanager.services.events.Event;
import com.servermanager.services.events.FileEventKey;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class StartServerManager {

	private static final String SAVE_FOLDER_NAME = "dat";
	private static final String CRYPTED_CONFIG = "sys.dat";
	private static FilesClusterService clusterService;
	private static final com.github.benmanes.caffeine.cache.Cache<FileEventKey, Event> handledEvents = Caffeine.<FileEventKey, Event>newBuilder().expireAfterWrite(EVENT_TIME_TO_LIVE, TimeUnit.MINUTES).build();

	public static void main(String[] args) throws Exception {
//		Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener(new FlavorListener() {
//			@Override
//			public void flavorsChanged(FlavorEvent e) {
//				System.out.println("changed!!! " + e.getSource() + " " + e.toString());
//			}
//		});
//		Thread.sleep(100000L);
//		ClipboardUtils.getClipboard();
//		ClipboardUtils.setClipboard("Hello Clipboard!!!");
//		WaitUtils.waitSomeTime(2);
//		WaitUtils.waitSomeTime(10000);
//		String host = "127.0.0.1";
//		int initialLocalPort = 48500;
//		int endPort = 48504;
//		int localPort = 48100;
//		try (Ignite ignite = IgniteUtils.createServerInstance(host, "first", initialLocalPort, endPort, localPort)) {
//			try (Ignite ignite2 = IgniteUtils.createServerInstance(host, "second", initialLocalPort, endPort, localPort)) {
//				try (Ignite ignite3 = IgniteUtils.createServerInstance(host, "third", initialLocalPort, endPort, localPort)) {
//					try (Ignite ignite4 = IgniteUtils.createServerInstance(host, "fourth", initialLocalPort, endPort, localPort)) {
//						IgniteCluster cluster = ignite.cluster();
//						int totalNodes = cluster.metrics().getTotalNodes();
//						System.out.println("totalNodes = " + totalNodes);
//					}
//				}
//			}
//		}
//		Path from = Paths.get("/home/me/GIT/ServerManager/ServerManager/target/ServerManager.jar");
//		Path to = Paths.get("/home/me/server_manager");
//		Path result = to.resolveSibling(from);
//		System.out.println(result);
//		SshClient sshClient = new SshClient("130.255.170.238", "me", "viking", 22, !currensOs.equals(OsEnum.AIX));
//		ExecCommandBean execCommand = sshClient.execCommand(new String[]{"cd /home/me/testDir"});
//		System.out.println(execCommand);
//		new SshUtils("130.255.170.238", "me", "viking", 22).uploadFiles(from, to);
		new ConfigHandler(SAVE_FOLDER_NAME, CRYPTED_CONFIG).start(args, argList2 -> argumentsHandler(args, argList2));
	}

	private static void argumentsHandler(String[] args, List<List<String>> argList2) {
		try {
			new SelfUpdateService().update(args);
			for (int i = 0; i < argList2.size(); i++) {
				List<String> argList = argList2.get(i);
				if (!argList.isEmpty()) {
					if (argList.get(0).equals("START")) {
						Integer port = Integer.valueOf(getParameter("-port", argList));
						ServerListerner serverListerner = new ServerListerner(port);
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
						clusterService = new FilesClusterService(hostMap.values(), instanceName, initialLocalPort, endPort, localPort, home, clientPort, clientPortRange);
						clusterService.startCluster();
					} else if (argList.get(0).equals("UPDATE")) {
						String host = getParameter("-host", argList);
						Integer port = Integer.valueOf(getParameter("-port", argList));
						new UpdateService(host, port).update(args);
					} else if (argList.get(0).equals("UPLOAD")) {
						String host = getParameter("-host", argList);
						Integer port = Integer.valueOf(getParameter("-port", argList));
						Path to = Paths.get(getParameter("-to", argList));
						Path from = Paths.get(getParameter("-from", argList));
						new UploadService(host, port).upload(to, from);
					} else if (argList.get(0).equals("SELF_UPDATE")) {
						new SelfUpdateService().update(args);
					} else if (argList.get(0).equals("DOWNLOAD")) {
						String host = getParameter("-host", argList);
						Integer port = Integer.valueOf(getParameter("-port", argList));
						Path from = Paths.get(getParameter("-from", argList));
						Path to = Paths.get(getParameter("-to", argList));
						new DownloadService(host, port).download(from, to);
					} else if (argList.get(0).equals("DELETE")) {
						String host = getParameter("-host", argList);
						Integer port = Integer.valueOf(getParameter("-port", argList));
						Path to = Paths.get(getParameter("-to", argList));
						new DeleteService(host, port).delete(to);
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
						new FilesClusterService(host, port).listFilesClient();
					} else if (argList.get(0).equals("WATCH")) {
						File dir = new File(getParameter("-dir", argList));
						String host = getParameter("-host", argList);
						Integer port = Integer.valueOf(getParameter("-port", argList));
						Path to = Paths.get(getParameter("-to", argList));
						initActionsBeforeCreatingTheListerners(host, port, to, dir);
						createFileSystemListerner(host, port, to, dir);
					} else if (argList.get(0).equals("EVENT")) {
						String host = getParameter("-host", argList);
						Integer port = Integer.valueOf(getParameter("-port", argList));
						File home = new File(getParameter("-dir", argList));
						String instanceName = getParameter("-instanceName", argList);
						Integer clientPort = Optional.ofNullable(getParameter("-clientPort", argList)).map(Integer::valueOf).orElse(null);
						Integer clientPortRange = Optional.ofNullable(getParameter("-portRange", argList)).map(Integer::valueOf).orElse(null);
						EventClusterService eventService = new EventClusterService(host, port, instanceName, clientPort, clientPortRange, home);
						eventService.startCluster();
						eventService.listenToFileEvents();
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
		System.out.println("Wrong arguments!");
		WaitUtils.waitSomeTime(20);
		createTestFile();
		System.out.println("Wrong arguments!");
	}

	private static void createTestFile() throws IOException {
		File file = new File("created");
		if (!file.exists()) {
			file.createNewFile();
		}
		Files.write(file.toPath(), (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n").getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
	}

	public static FilesClusterService getClusterService() {
		return clusterService;
	}

	public static Cache<FileEventKey, Event> getHandledEvents() {
		return handledEvents;
	}
}
