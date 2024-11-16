package ru.kiokle.simplehttpserver.clients;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.List;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.BUFFER_SIZE;

public abstract class I2pClient extends Client {

    protected final int proxyPort;

    public I2pClient(List<String> argList, String destinationHost, int destinationPort, int proxyPort) {
        super(argList, destinationHost, destinationPort);
        this.proxyPort = proxyPort;
    }

    @Override
    public void connect() throws Exception {
        SocketAddress proxyAddress = new InetSocketAddress("localhost", proxyPort);
        Proxy proxy = new Proxy(Proxy.Type.HTTP, proxyAddress);
        try (Socket socket = new Socket(proxy)) {
            socket.connect(new InetSocketAddress(destinationHost, destinationPort));
            try (BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
                    BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE)) {
                handle(outputStream, inputStream);
            }
        }
    }
}
