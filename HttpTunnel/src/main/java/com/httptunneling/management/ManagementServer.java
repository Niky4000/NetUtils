package com.httptunneling.management;

import com.httptunneling.TunnelStart;
import static com.httptunneling.TunnelStart.addOpenedPort;
import static com.httptunneling.TunnelStart.interruptEverything;
import static com.httptunneling.TunnelStart.updateFireWall;
import static com.httptunneling.utils.NetUtils.parceFilters;
import static com.httptunneling.utils.NetUtils.readInputStream;
import com.lib.ConfigHandler;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ManagementServer {

	public static final String FIREWALL = "FIREWALL";

	public void init(int sourcePort) {
		try {
			ServerSocket serverSocket = new ServerSocket(sourcePort);
			addOpenedPort(null, serverSocket);
			while (true) {
				try (Socket clientSocket = serverSocket.accept()) {
					ByteArrayOutputStream byteArrayOutputStream = readInputStream(() -> clientSocket);
					String string = new String(byteArrayOutputStream.toByteArray());
					if (string.length() > 0 && string.contains(" ")) {
						String[] args = string.trim().split(" ");
						if (args.length > 0 && args[0].equals(FIREWALL)) {
							updateFireWall(parceFilters(new ArrayList<>(Arrays.asList(args))));
						} else {
							List<List<String>> argList2 = ConfigHandler.getArgList(args);
							interruptEverything(args, argList2, Thread.currentThread());
							break;
						}
					} else {
						continue;
					}
				} catch (Exception e) {
					continue;
				}
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
}
