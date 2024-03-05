package ru.kiokle.simplehttpserver.clients;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.BUFFER_SIZE;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.endStr;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.headEndStr;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.startOfStream;

public abstract class I2pClient {

    protected final List<String> argList;
    protected final String destinationHost;
    protected final int destinationPort;
    protected final int proxyPort;

    public I2pClient(List<String> argList, String destinationHost, int destinationPort, int proxyPort) {
        this.argList = argList;
        this.destinationHost = destinationHost;
        this.destinationPort = destinationPort;
        this.proxyPort = proxyPort;
    }

    public abstract void handle(BufferedOutputStream outputStream, BufferedInputStream inputStream) throws Exception;

    String readInputStream(BufferedInputStream inputStream) throws IOException {
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

    protected byte[] makeHeadBytes(Supplier<String> command) {
        StringBuilder stringBuilder = new StringBuilder("POST http://" + destinationHost + "/ HTTP/1.1\n"
                + "Host: " + destinationHost + "\n");
        stringBuilder.append(startOfStream);
        stringBuilder.append(endStr);
        stringBuilder.append("User-Agent: curl/8.2.1\n"
                + "Accept: */*\n"
                + "Proxy-Connection: Keep-Alive" + headEndStr);
        stringBuilder.append(command.get());
        byte[] bytes = stringBuilder.toString().getBytes();
        return bytes;
    }
}
