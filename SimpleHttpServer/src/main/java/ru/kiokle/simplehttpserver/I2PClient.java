package ru.kiokle.simplehttpserver;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import net.i2p.I2PException;
import net.i2p.client.I2PSession;
import net.i2p.client.streaming.I2PServerSocket;
import net.i2p.client.streaming.I2PSocket;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.client.streaming.I2PSocketManagerFactory;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.BUFFER_SIZE;

public class I2PClient {

    // curl -v --proxy localhost:4444 http://mgfomi116.i2p/
    public void connect(String destinationHost, int destinationPort, int proxyPort) throws IOException {
        String readInputStream = null;
        InetSocketAddress hiddenerProxyAddress = new InetSocketAddress("127.0.0.1", proxyPort);
        Proxy hiddenProxy = new Proxy(Proxy.Type.HTTP, hiddenerProxyAddress);
        try (Socket socket = new Socket(hiddenProxy)) {
            InetSocketAddress sa = InetSocketAddress.createUnresolved(destinationHost, destinationPort);
            socket.connect(sa);
            socket.setKeepAlive(true);
            try (BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE)) {
                readInputStream = readInputStream(inputStream);
            }
        }
        System.out.println(readInputStream != null ? readInputStream : "readInputStream is empty!");
    }

    public void connect2(String destinationHost, int destinationPort, int proxyPort) throws IOException, DataFormatException, I2PException {
        I2PSocketManager manager = I2PSocketManagerFactory.createManager();
        I2PSocket socket = manager.connect(new Destination(destinationHost));
        String readInputStream = readInputStream(new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE));
        System.out.println(readInputStream != null ? readInputStream : "readInputStream is empty!");
    }

    public void createServer() {
        I2PSocketManager manager = I2PSocketManagerFactory.createManager();
        I2PServerSocket serverSocket = manager.getServerSocket();
        I2PSession session = manager.getSession();
        //Print the base64 string, the regular string would look like garbage.
        System.out.println(session.getMyDestination().toBase64());
    }

    private String readInputStream(BufferedInputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int read = 0;
        do {
            byte[] buffer = new byte[BUFFER_SIZE];
            read = inputStream.read(buffer);
            if (read > 0) {
                byteArrayOutputStream.write(buffer, 0, read);
            }
        } while (read > 0);
        return new String(byteArrayOutputStream.toByteArray());
    }
}
