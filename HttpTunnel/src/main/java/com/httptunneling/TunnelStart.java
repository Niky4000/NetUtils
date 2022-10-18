package com.httptunneling;

import com.httptunneling.management.ManagementServer;
import static com.httptunneling.management.ManagementServer.FIREWALL;
import static com.httptunneling.utils.NetUtils.parceFilters;
import static com.httptunneling.utils.NetUtils.readInputStream;
import static com.httptunneling.utils.NetUtils.writeOutputStream;
import com.lib.ConfigHandler;
import com.some.tcp.TCPForwardClientR;
import com.some.tcp.TCPForwardServer;
import com.some.tcp.TCPForwardServerOnion;
import com.some.tcp.TCPForwardServerR2;
import com.some.tcp.TCPIPServer;
import static com.utils.Utils.join;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author NAnishhenko
 */
public class TunnelStart {

	private static final String SAVE_FOLDER_NAME = "sys";
	private static final String CRYPTED_CONFIG = "sys.dat";

	public static void main(String[] args) throws Exception {
		new ConfigHandler(SAVE_FOLDER_NAME, CRYPTED_CONFIG).start(args, (args_, argList2) -> argumentsHandler(args_, argList2));
	}

	private static Map<Integer, Set<String>> filterMap = new ConcurrentHashMap<>();
	private static Set<Integer> openedPorts = new CopyOnWriteArraySet<>();
	private static List<ServerSocket> serverSockets = new CopyOnWriteArrayList<>();

	public static void argumentsHandler(String[] args, List<List<String>> argList2) {
		if (args[0].equals("S")) {
			List<String> argList = Arrays.asList(args);
			List<String> subList = new ArrayList<>(argList.subList(3, argList.size()));
			String dataToWrite = subList.stream().reduce("", (s1, s2) -> s1 + " " + s2);
			writeOutputStream(() -> createSocket(argList), dataToWrite.getBytes());
		} else {
			for (int i = 0; i < argList2.size(); i++) {
				List<String> argList = argList2.get(i);
				filterMap.putAll(parceFilters(argList));
				if (argList.get(0).equals("M")) { // Management thread
					m(new Thread(() -> new ManagementServer().init(Integer.valueOf(argList.get(1)))));
				} else if (argList.get(0).equals("L")) {
//        new TCPForwardServer().init(22888, "192.168.192.215", 22);
					h(new Thread(() -> new TCPForwardServer().init(Integer.valueOf(argList.get(1)), argList.get(2), Integer.valueOf(argList.get(3)), filterMap)), i);
				} else if (argList.get(0).equals("LO")) {
//        new TCPForwardServer().init(22888, "192.168.192.215", 22);
					h(new Thread(() -> new TCPForwardServerOnion().init(Integer.valueOf(argList.get(1)), argList.get(2), Integer.valueOf(argList.get(3)), filterMap)), i);
				} else if (argList.get(0).equals("R")) {
//            new TCPForwardServerR().init(22777, 22888);
					h(new Thread(() -> new TCPForwardServerR2().init(Integer.valueOf(argList.get(1)), Integer.valueOf(argList.get(2)), filterMap)), i);
				} else if (argList.get(0).equals("RC")) {
//            new TCPForwardClientR().init("192.168.192.216", 22888, "172.29.4.26", 22);
					h(new Thread(() -> new TCPForwardClientR().init(argList.get(1), Integer.valueOf(argList.get(2)), argList.get(3), Integer.valueOf(argList.get(4)))), i);
				} else if (argList.get(0).equals("IP")) {
					ip(new Thread(() -> new TCPIPServer().init(Integer.valueOf(argList.get(1)))));
				} else if (argList.get(0).equals("PING")) {
					ByteArrayOutputStream byteArrayOutputStream = readInputStream(() -> createSocket(argList));
					System.out.println(new String(byteArrayOutputStream.toByteArray()));
				} else if (argList.get(0).equals("PING2")) {
					ByteArrayOutputStream byteArrayOutputStream = readInputStream(() -> createSocket(argList));
					String myIp = new String(byteArrayOutputStream.toByteArray());
					System.out.println(myIp);
					List<String> subList = new ArrayList<>(argList.subList(4, argList.size()));
					String additionalIps = subList.stream().reduce("", (s1, s2) -> s1 + " " + s2).trim();
					writeOutputStream(() -> createSocket2(argList), (FIREWALL + " " + "0 F " + myIp + " " + additionalIps).getBytes());
				}
			}
		}
	}

	private static Socket createSocket(List<String> argList) {
		try {
			return new Socket(argList.get(1), Integer.valueOf(argList.get(2)));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private static Socket createSocket2(List<String> argList) {
		try {
			return new Socket(argList.get(1), Integer.valueOf(argList.get(3)));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private static void createLocalSocket(int port) {
		try {
			Socket socket = new Socket("127.0.0.1", port);
			writeOutputStream(() -> socket, "".getBytes());
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private static final String IPV4_PATTERN = "^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!$)|$)){4}$";
	private static final Pattern pattern = Pattern.compile(IPV4_PATTERN);

	public static boolean isIpAddressValid(final String ipAddress) {
		Matcher matcher = pattern.matcher(ipAddress);
		return matcher.matches();
	}

	private static void h(Thread thread, int i) {
		thread.setName("TunnelStart_" + i);
		threadList.add(thread);
		thread.start();
	}

	private static void m(Thread thread) {
		thread.setName("ManagementThread");
		threadList.add(thread);
		thread.start();
	}

	private static void ip(Thread thread) {
		thread.setName("IP");
		threadList.add(thread);
		thread.start();
	}

	private static void initThread(Thread thread) {
		thread.setName("InitThread");
		thread.start();
	}

	public static void addOpenedPort(Integer port, ServerSocket serverSocket) {
		if (port != null) {
			openedPorts.add(port);
		}
		if (serverSocket != null) {
			serverSockets.add(serverSocket);
		}
	}

	private static void closeServerSocket(ServerSocket serverSocket) {
		try {
			serverSocket.close();
		} catch (IOException ex) { // Ignore it!
		}
	}

	private static List<Thread> threadList = new CopyOnWriteArrayList<>();
	private static AtomicBoolean interruptEverything = new AtomicBoolean(false);

	public static boolean isEverythingInterrupted() {
		return interruptEverything.get();
	}

	public static void updateFireWall(Map<Integer, Set<String>> map) {
		if (map.size() == 1 && map.containsKey(0)) {
			filterMap.keySet().forEach(key -> filterMap.compute(key, (k, v) -> map.get(0)));
		} else {
			filterMap.putAll(map);
		}
	}

	public static void interruptEverything(String[] args, final List<List<String>> argList2, final Thread thread) {
		interruptEverything.set(true);
		initThread(new Thread(() -> {
			join(thread);
			filterMap.clear();
			openedPorts.forEach(port -> createLocalSocket(port));
			threadList.forEach(thread_ -> thread_.interrupt());
			threadList.forEach(thread_ -> join(thread_));
			serverSockets.forEach(serverSocket -> closeServerSocket(serverSocket));
			openedPorts.clear();
			serverSockets.clear();
			threadList.clear();
			interruptEverything.set(false);
			try {
				new ConfigHandler(SAVE_FOLDER_NAME, CRYPTED_CONFIG).cryptConfigs(Arrays.asList(args));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			argumentsHandler(args, argList2);
		}));
	}
}
