package ru.kiokle.simplehttpserver.clients;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.List;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.delimiter;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.endOfStream;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.endStr;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.getConfig;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.headEndStr;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.startOfStream;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.EXEC;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.LENGTH;

public class ExecCommandClient extends I2pClient {

    public ExecCommandClient(List<String> argList, String destinationHost, int destinationPort, int proxyPort) {
        super(argList, destinationHost, destinationPort, proxyPort);
    }

    public static String createCommand(String command) {
        return EXEC.name() + delimiter + command + endStr + LENGTH.name() + delimiter + "0" + headEndStr;
    }

    @Override
    public void handle(BufferedOutputStream outputStream, BufferedInputStream inputStream) throws Exception {
        StringBuilder stringBuilder = new StringBuilder("POST http://" + destinationHost + "/ HTTP/1.1\n"
                + "Host: " + destinationHost + "\n");
        stringBuilder.append(startOfStream);
        stringBuilder.append(endStr);
        stringBuilder.append("User-Agent: curl/8.2.1\n"
                + "Accept: */*\n"
                + "Proxy-Connection: Keep-Alive" + headEndStr);
        String command = getConfig("-command", argList);
        stringBuilder.append(createCommand(command));
        byte[] bytes = stringBuilder.toString().getBytes();
        outputStream.write(bytes);
        outputStream.write(endOfStream);
        outputStream.flush();
        String readInputStream = readInputStream(inputStream);
        System.out.println(readInputStream);
    }
}
