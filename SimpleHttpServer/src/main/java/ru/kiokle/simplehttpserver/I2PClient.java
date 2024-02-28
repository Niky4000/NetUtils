package ru.kiokle.simplehttpserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.BUFFER_SIZE;
import ru.kiokle.simplehttpserver.clients.I2pClient;

public class I2PClient {

    // curl -v --proxy localhost:4444 http://mgfomi116.i2p/
//    public void connect(String destinationHost, int destinationPort, int proxyPort) throws IOException {
//        String readInputStream = null;
//        InetSocketAddress hiddenerProxyAddress = new InetSocketAddress("127.0.0.1", proxyPort);
//        Proxy hiddenProxy = new Proxy(Proxy.Type.HTTP, hiddenerProxyAddress);
//        try (Socket socket = new Socket(hiddenProxy)) {
//            InetSocketAddress sa = InetSocketAddress.createUnresolved(destinationHost, destinationPort);
//            socket.setSoTimeout(100000);
//            socket.connect(sa);
//            socket.setKeepAlive(true);
//            try (BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE)) {
//                readInputStream = readInputStream(inputStream);
//            }
//        }
//        System.out.println(readInputStream != null ? readInputStream : "readInputStream is empty!");
//    }
//    public void connect2(String destinationHost, int destinationPort, int proxyPort) throws IOException, DataFormatException, I2PException {
//        I2PSocketManager manager = I2PSocketManagerFactory.createManager();
//        I2PSocket socket = manager.connect(new Destination(destinationHost));
//        String readInputStream = readInputStream(new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE));
//        System.out.println(readInputStream != null ? readInputStream : "readInputStream is empty!");
//    }
    public void connect(String destinationHost, int destinationPort, int proxyPort, I2pClient i2pClient) throws Exception {
        String readInputStream = null;
        SocketAddress proxyAddress = new InetSocketAddress("localhost", proxyPort);
        Proxy proxy = new Proxy(Proxy.Type.HTTP, proxyAddress);
        try (Socket socket = new Socket(proxy)) {
            socket.connect(new InetSocketAddress(destinationHost, destinationPort));
            try (BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
                    BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE)) {
                i2pClient.handle(outputStream, inputStream);
            }
        }
    }

//    public void createI2PServer() {
//        I2PSocketManager manager = I2PSocketManagerFactory.createManager();
//        I2PServerSocket serverSocket = manager.getServerSocket();
//        I2PSession session = manager.getSession();
//        //Print the base64 string, the regular string would look like garbage.
//        System.out.println(session.getMyDestination().toBase64());
//    }
}
