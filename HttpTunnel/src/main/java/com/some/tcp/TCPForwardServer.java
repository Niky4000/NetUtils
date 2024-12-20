package com.some.tcp;

/**
 * This program is an example from the book "Internet programming with Java" by
 * Svetlin Nakov. It is freeware. For more information:
 * http://www.nakov.com/books/inetjava/
 */
import com.httptunneling.TunnelStart;
import static com.httptunneling.TunnelStart.addOpenedPort;
import static com.httptunneling.TunnelStart.isEverythingInterrupted;
import static com.httptunneling.utils.NetUtils.handleError;
import static com.utils.Utils.fireWall;
import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.Set;

/**
 * TCPForwardServer is a simple TCP bridging software that allows a TCP port on
 * some host to be transparently forwarded to some other TCP port on some other
 * host. TCPForwardServer continuously accepts client connections on the
 * listening TCP port (source port) and starts a thread (ClientThread) that
 * connects to the destination host and starts forwarding the data between the
 * client socket and destination socket.
 */
public class TCPForwardServer {

//    public static final int SOURCE_PORT = 22888;
//    public static final String DESTINATION_HOST = "mail.abv.bg";
//    public static final int DESTINATION_PORT = 25;
//    public static final String DESTINATION_HOST = "192.168.192.215";
//    public static final int DESTINATION_PORT = 22;
//    public static void main(String[] args) throws IOException {
//        new TCPForwardServer().init(22888, "192.168.192.215", 22);
//    }
    public void init(int sourcePort, String destinationHost, int destinationPort, Map<Integer, Set<String>> filterMap) {
        try {
            ServerSocket serverSocket = new ServerSocket(sourcePort);
            addOpenedPort(sourcePort, serverSocket);
            while (!isEverythingInterrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    if (fireWall(sourcePort, clientSocket, filterMap)) {
                        clientSocket.close();
                        continue;
                    }
                    // Connect to the destination server 
                    Socket mServerSocket = new Socket(destinationHost, destinationPort);
                    // Turn on keep-alive for both the sockets 
                    mServerSocket.setKeepAlive(true);
                    clientSocket.setKeepAlive(true);
                    ClientThread clientThread = new ClientThread(clientSocket, mServerSocket);
                    clientThread.start();
                } catch (IOException ex) {
                    if (handleError(ex, "L")) {
                        break;
                    }
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
