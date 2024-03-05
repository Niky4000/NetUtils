package ru.kiokle.simplehttpserver.clients;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.List;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.delimiter;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.endOfStream;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.endStr;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.getConfig;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.headEndStr;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.LENGTH;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.MD5;

public class Md5Client extends I2pClient {

    public Md5Client(List<String> argList, String destinationHost, int destinationPort, int proxyPort) {
        super(argList, destinationHost, destinationPort, proxyPort);
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
        System.out.println(readInputStream);
    }
}
