package ru.kiokle.simplehttpserver.clients;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.delimiter;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.endOfStream;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.endStr;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.getConfig;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.headEndStr;
import ru.kiokle.simplehttpserver.clients.enums.ConnectionType;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.LENGTH;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.MD5;
import ru.kiokle.simplehttpserver.log.Logger;

public class Md5Client extends Client {

    private final AtomicReference<String> md5Reference;

    public Md5Client(List<String> argList, String destinationHost, int destinationPort, int proxyPort, ConnectionType connectionType, AtomicReference<String> md5Reference) {
        super(argList, destinationHost, destinationPort, proxyPort, connectionType);
        this.md5Reference = md5Reference;
    }

    public static String createCommand(String command) {
        return MD5.name() + delimiter + command + endStr + LENGTH.name() + delimiter + "0" + headEndStr;
    }

    @Override
    public void handle(BufferedOutputStream outputStream, BufferedInputStream inputStream) throws Exception {
        outputStream.write(makeHeadBytes(() -> createCommand(getConfig("-file", argList))));
        outputStream.write(endOfStream);
        outputStream.flush();
        String readInputStream = readInputStream(inputStream);
        md5Reference.set(readInputStream.substring(readInputStream.lastIndexOf(endStr) + endStr.length()));
        Logger.log(readInputStream);
    }
}
