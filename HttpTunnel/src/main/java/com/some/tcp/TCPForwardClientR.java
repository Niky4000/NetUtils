package com.some.tcp;

import static com.httptunneling.TunnelStart.isEverythingInterrupted;
import static com.httptunneling.utils.NetUtils.handleError;
import static com.httptunneling.utils.WaitUtils.waitSomeTime;
import java.net.Socket;

/**
 * @author NAnishhenko
 */
public class TCPForwardClientR {

//    public static void main(String[] args) throws IOException {
//        new TCPForwardClientR().init("192.168.192.216", 22888, "172.29.4.26", 22);
//    }
    public void init(String sourceHost, int sourcePort, String destinationHost, int destinationPort) {
        while (!isEverythingInterrupted()) {
            Socket clientSocket = null;
            Socket mServerSocket = null;
            try {
                clientSocket = new Socket(sourceHost, sourcePort);
                Logger.log("clientSocket " + clientSocket.toString() + " connected!");
                mServerSocket = new Socket(destinationHost, destinationPort);
                Logger.log("mServerSocket " + mServerSocket.toString() + " connected!");
                // Turn on keep-alive for both the sockets 
                mServerSocket.setKeepAlive(true);
                clientSocket.setKeepAlive(true);
                ClientThread clientThread = new ClientThread(clientSocket, mServerSocket);
                clientThread.start();
                clientThread.getSemaphore().acquire();
            } catch (Exception e) {
                if (handleError(e, "RC")) {
                    break;
                }
            } finally {
                closeSocket(clientSocket);
                closeSocket(mServerSocket);
            }
            waitSomeTime(2);
        }
    }

    private void closeSocket(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
                com.some.tcp.Logger.log(e.getMessage());
            }
        }
    }
}
