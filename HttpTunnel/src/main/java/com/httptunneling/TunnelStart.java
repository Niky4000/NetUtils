package com.httptunneling;

import com.lib.ConfigHandler;
import com.some.tcp.TCPForwardClientR;
import com.some.tcp.TCPForwardServer;
import com.some.tcp.TCPForwardServerOnion;
import com.some.tcp.TCPForwardServerR2;
import com.some.tcp.TCPIPServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
		new ConfigHandler(SAVE_FOLDER_NAME, CRYPTED_CONFIG).start(args, argList2 -> argumentsHandler(args, argList2));
	}

	private static void argumentsHandler(String[] args, List<List<String>> argList2) {
		for (int i = 0; i < argList2.size(); i++) {
			List<String> argList = argList2.get(i);
			Map<Integer, Set<String>> filterMap = parceFilters(argList);
			if (argList.get(0).equals("L")) {
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
				h(new Thread(() -> new TCPIPServer().init(Integer.valueOf(argList.get(1)))), i);
			} else if (argList.get(0).equals("PING")) {
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				try (Socket socket = new Socket(argList.get(1), Integer.valueOf(argList.get(2)))) {
					try (InputStream inputStream = socket.getInputStream()) {
						int read = 0;
						do {
							byte[] buffer = new byte[BUFFER_SIZE];
							read = inputStream.read(buffer);
							byteArrayOutputStream.write(buffer, 0, read);
							if (read < BUFFER_SIZE) {
								break;
							}
						} while (read > 0);
					}
				} catch (IOException ex) {
					Logger.getLogger(TunnelStart.class.getName()).log(Level.SEVERE, null, ex);
				}
				System.out.println(new String(byteArrayOutputStream.toByteArray()));
			}
		}
	}

	private static final int BUFFER_SIZE = 1024;
	private static final String IPV4_PATTERN = "^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!$)|$)){4}$";
	private static final Pattern pattern = Pattern.compile(IPV4_PATTERN);

	public static boolean isIpAddressValid(final String ipAddress) {
		Matcher matcher = pattern.matcher(ipAddress);
		return matcher.matches();
	}

	private static Map<Integer, Set<String>> parceFilters(List<String> argList) {
		Map<Integer, Set<String>> map = new HashMap<>();
		List<Integer> argsToRemove = new ArrayList<>();
		for (int j = 0; j < argList.size(); j++) {
			if (argList.get(j).equals("F")) {
				argsToRemove.add(j);
				Set<String> ipSet = new HashSet<>();
				for (int i = j + 1; i < argList.size(); i++) {
					if (isIpAddressValid(argList.get(i))) {
						argsToRemove.add(i);
						ipSet.add(argList.get(i).replace("*", ""));
					} else {
						break;
					}
				}
				map.put(Integer.valueOf(argList.get(j - 1)), ipSet);
			}
		}
		for (int i = argsToRemove.size() - 1; i >= 0; i--) {
			argList.remove(argsToRemove.get(i).intValue());
		}
		return map;
	}

	private static void h(Thread thread, int i) {
		thread.setName("TunnelStart_" + i);
		thread.start();
	}
}
