package ru.kiokle.simplehttpserver.clients;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.List;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.delimiter;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.endOfStream;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.endStr;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.headEndStr;
import ru.kiokle.simplehttpserver.clients.enums.ConnectionType;
import static ru.kiokle.simplehttpserver.clients.enums.ConnectionType.LOCAL;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.LENGTH;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.STOP;
import ru.kiokle.simplehttpserver.log.Logger;

public class StopServerClient extends Client {

    public StopServerClient(List<String> argList, String destinationHost, int destinationPort) {
        super(argList, destinationHost, destinationPort, 0, LOCAL);
    }

    public static String createCommand() {
        return STOP.name() + delimiter + " " + endStr + LENGTH.name() + delimiter + "0" + headEndStr;
    }

    @Override
    public void handle(BufferedOutputStream outputStream, BufferedInputStream inputStream) throws Exception {
        outputStream.write(makeHeadBytes(() -> createCommand()));
        outputStream.write(endOfStream);
        outputStream.flush();
        String readInputStream = readInputStream(inputStream);
        Logger.log(readInputStream);
    }
}
