package ru.kiokle.simplehttpserver.clients;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.delimiter;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.endOfStream;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.endStr;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.headEndStr;
import ru.kiokle.simplehttpserver.clients.enums.ConnectionType;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.LENGTH;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.LOG;
import ru.kiokle.simplehttpserver.log.Logger;

public class LogClient extends Client {

    private final AtomicReference<String> logsReference;
    private final int count;

    public LogClient(List<String> argList, String destinationHost, int destinationPort, int proxyPort, ConnectionType connectionType, int count, AtomicReference<String> md5Reference) {
        super(argList, destinationHost, destinationPort, proxyPort, connectionType);
        this.logsReference = md5Reference;
        this.count = count;
    }

    public static String createCommand(int count) {
        return LOG.name() + delimiter + count + endStr + LENGTH.name() + delimiter + "0" + headEndStr;
    }

    @Override
    public void handle(BufferedOutputStream outputStream, BufferedInputStream inputStream) throws Exception {
        outputStream.write(makeHeadBytes(() -> createCommand(count)));
        outputStream.write(endOfStream);
        outputStream.flush();
        String readInputStream = readInputStream(inputStream);
        logsReference.set(readInputStream.substring(readInputStream.lastIndexOf(endStr) + endStr.length()));
        Logger.log(readInputStream);
    }
}
