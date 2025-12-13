package ru.kiokle.simplehttpserver.clients;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.List;
import ru.kiokle.simplehttpserver.clients.enums.ConnectionType;
import ru.kiokle.simplehttpserver.log.Logger;

public class PingClient extends Client {

    public PingClient(List<String> argList, String destinationHost, int destinationPort, int proxyPort, ConnectionType connectionType) {
        super(argList, destinationHost, destinationPort, proxyPort, connectionType);
    }

    @Override
    public void handle(BufferedOutputStream outputStream, BufferedInputStream inputStream) throws Exception {
        String str = "GET http://" + destinationHost + "/ HTTP/1.1\n"
                + "Host: " + destinationHost + "\n"
                + "User-Agent: curl/8.2.1\n"
                + "Accept: */*\n"
                + "Proxy-Connection: Keep-Alive\n\n";
        outputStream.write(str.getBytes());
        outputStream.flush();
        String readInputStream = readInputStream(inputStream);
        Logger.log(readInputStream != null ? readInputStream : "readInputStream is empty!");
    }
}
