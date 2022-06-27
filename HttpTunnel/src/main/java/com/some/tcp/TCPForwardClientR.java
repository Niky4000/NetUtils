package com.some.tcp;

import static com.some.tcp.DateUtils.getDateStr;
import java.net.Socket;
import java.util.logging.Level;

/**
 * @author NAnishhenko
 */
public class TCPForwardClientR {

    private static final int WAIT_TIME = 10 * 1000;
//    public static void main(String[] args) throws IOException {
//        new TCPForwardClientR().init("192.168.192.216", 22888, "172.29.4.26", 22);
//    }

    public void init(String sourceHost, int sourcePort, String destinationHost, int destinationPort) {
        while (true) {
            try {
                Socket clientSocket = new Socket(sourceHost, sourcePort);
                Socket mServerSocket = new Socket(destinationHost, destinationPort);
                // Turn on keep-alive for both the sockets 
                mServerSocket.setKeepAlive(true);
                clientSocket.setKeepAlive(true);
                ClientThread clientThread = new ClientThread(clientSocket, mServerSocket);
                clientThread.start();
                clientThread.getSemaphore().acquire();
            } catch (Exception e) {
				Logger.log(getDateStr());
                Logger.log("Exception in RC mode!" + (e.getMessage() != null ? " Exception Message: " + e.getMessage() : ""));
				try {
					Thread.sleep(WAIT_TIME);
				} catch (InterruptedException ex) {
					java.util.logging.Logger.getLogger(TCPForwardClientR.class.getName()).log(Level.SEVERE, null, ex);
				}
            }
        }
    }
}
