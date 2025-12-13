package ru.kiokle.simplehttpserver.clients;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.List;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.delimiter;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.endOfStream;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.endStr;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.headEndStr;
import ru.kiokle.simplehttpserver.clients.enums.ConnectionType;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.EXEC;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.LENGTH;
import ru.kiokle.simplehttpserver.log.Logger;

public class ExecCommandClient extends Client {

    private final String command;

    public ExecCommandClient(List<String> argList, String destinationHost, int destinationPort, int proxyPort, ConnectionType connectionType, String command) {
        super(argList, destinationHost, destinationPort, proxyPort, connectionType);
        this.command = command;
    }

    public static String createCommand(String command) {
        return EXEC.name() + delimiter + command + endStr + LENGTH.name() + delimiter + "0" + headEndStr;
    }

    @Override
    public void handle(BufferedOutputStream outputStream, BufferedInputStream inputStream) throws Exception {
        outputStream.write(makeHeadBytes(() -> createCommand(command)));
        outputStream.write(endOfStream);
        outputStream.flush();
        String readInputStream = readInputStream(inputStream);
        Logger.log(readInputStream);
    }
}
