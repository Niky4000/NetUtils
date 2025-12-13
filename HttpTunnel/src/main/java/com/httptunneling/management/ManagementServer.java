package com.httptunneling.management;

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

public class ManagementServer {

    public static final String FIREWALL = "FIREWALL";
    public static final String CHANGE_CONFIG = "CHANGE_CONFIG";
    private static final int TIMEOUT = 2000;

    public void init(int sourcePort) {
        try {
            ServerSocket serverSocket = new ServerSocket(sourcePort);
            addOpenedPort(null, serverSocket);
            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    clientSocket.setSoTimeout(TIMEOUT);
                    ByteArrayOutputStream byteArrayOutputStream = readInputStream(() -> clientSocket);
                    String string = new String(byteArrayOutputStream.toByteArray());
                    if (string.length() > 0 && string.contains(" ")) {
                        String[] args = string.trim().split(" ");
                        if (args.length > 0 && args[0].equals(FIREWALL)) {
                            updateFireWall(parceFilters(new ArrayList<>(Arrays.asList(args))));
                        } else if (args.length > 0 && args[0].equals(CHANGE_CONFIG)) {
                            String[] args2 = Arrays.asList(args).subList(1, args.length).toArray(new String[1]);
                            List<List<String>> argList2 = ConfigHandler.getArgList(args2);
                            interruptEverything(args2, argList2, Thread.currentThread());
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
