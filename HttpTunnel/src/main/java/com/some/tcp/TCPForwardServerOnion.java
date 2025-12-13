package com.some.tcp;

import static com.httptunneling.TunnelStart.addOpenedPort;
import static com.httptunneling.TunnelStart.isEverythingInterrupted;
import static com.httptunneling.utils.NetUtils.handleError;
import static com.utils.Utils.fireWall;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author me
 */
public class TCPForwardServerOnion extends TCPForwardServer {

    @Override
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
//					// Connect to the destination server 
//					Socket mServerSocket = new Socket(destinationHost, destinationPort);
//					// Turn on keep-alive for both the sockets 
//					mServerSocket.setKeepAlive(true);
//					clientSocket.setKeepAlive(true);
//					ClientThread clientThread = new ClientThread(clientSocket, mServerSocket);
//					clientThread.start();
                    InetSocketAddress hiddenerProxyAddress = new InetSocketAddress("127.0.0.1", 9050);
                    Proxy hiddenProxy = new Proxy(Proxy.Type.SOCKS, hiddenerProxyAddress);
                    Socket mServerSocket = new Socket(hiddenProxy);
//					SocketAddress sa = new InetSocketAddress("www.facebook.com", 80);
                    InetSocketAddress sa = InetSocketAddress.createUnresolved(destinationHost, destinationPort);
                    mServerSocket.connect(sa);
//					Socket mServerSocket = new Socket(destinationHost, destinationPort);
                    // Turn on keep-alive for both the sockets 
                    mServerSocket.setKeepAlive(true);
                    clientSocket.setKeepAlive(true);
                    ClientThread clientThread = new ClientThread(clientSocket, mServerSocket);
                    clientThread.start();
                } catch (IOException ex) {
                    if (handleError(ex, "LO")) {
                        break;
                    }
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
