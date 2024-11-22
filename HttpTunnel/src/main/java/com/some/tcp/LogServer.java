package com.some.tcp;

import static com.httptunneling.TunnelStart.addOpenedPort;
import static com.httptunneling.TunnelStart.isEverythingInterrupted;
import static com.utils.Utils.fireWall;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LogServer {

    public static final int LOCAL_BUFFER_SIZE = 1024 * 1024;

    public void init(int port, Map<Integer, Set<String>> filterMap) {
        while (true) {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(port);
                addOpenedPort(port, serverSocket);
                while (!isEverythingInterrupted()) {
                    try (Socket clientSocket = serverSocket.accept()) {
                        if (fireWall(port, clientSocket, filterMap)) {
                            clientSocket.close();
                            continue;
                        }
                        // Turn on keep-alive for both the sockets 
                        clientSocket.setKeepAlive(true);
                        List<String> messages = Logger.getMessages();
                        try (BufferedOutputStream outputStream = new BufferedOutputStream(clientSocket.getOutputStream(), LOCAL_BUFFER_SIZE);) {
                            StringBuilder sb = new StringBuilder("<html><head><title>Logs</title></head><body>");
                            for (String message : messages) {
                                sb.append(message + "<br>\n");
                            }
                            sb.append("</body></html>");
                            makeStandartOutput(outputStream, sb.toString());
                        }
                    }
                }
            } catch (Exception e) {
                Logger.log(e);
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (Exception ee) {
                        Logger.log(ee);
                    }
                }
                if (isEverythingInterrupted()) {
                    break;
                }
            }
        }
    }

    public static void makeStandartOutput(final BufferedOutputStream outputStream, String dataStr) throws IOException {
        byte[] data = dataStr.getBytes();
        String headers = "HTTP/1.1 200\n"
                + "content-length: " + data.length + "\n"
                + "cache-control: no-cache\n"
                + "content-type: text/html\n"
                + "connection: close\n\n";
        outputStream.write(headers.getBytes());
        outputStream.write(data);
        outputStream.flush();
    }

    StringBuilder getHeaders() {
        return new StringBuilder("HTTP/1.1 200\n"
                + "cache-control: no-cache\n"
                + "content-type: text/html\n"
                + "connection: close\n\n");
    }
}
