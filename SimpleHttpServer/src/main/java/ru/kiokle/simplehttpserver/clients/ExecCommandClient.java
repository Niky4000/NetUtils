package ru.kiokle.simplehttpserver.clients;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.List;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.delimiter;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.endOfStream;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.endStr;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.getConfig;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.headEndStr;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.EXEC;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.LENGTH;

public class ExecCommandClient extends I2pClient {

    private final String command;

    public ExecCommandClient(List<String> argList, String destinationHost, int destinationPort, int proxyPort, String command) {
        super(argList, destinationHost, destinationPort, proxyPort);
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
        System.out.println(readInputStream);
    }
}
