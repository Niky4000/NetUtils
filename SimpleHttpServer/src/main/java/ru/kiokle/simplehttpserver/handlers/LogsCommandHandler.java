package ru.kiokle.simplehttpserver.handlers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.endStr;
import ru.kiokle.simplehttpserver.log.Logger;

public class LogsCommandHandler implements CommandHandler {

    @Override
    public void handle(ByteArrayOutputStream byteArrayOutputStream, BufferedInputStream inputStream, int length, BufferedOutputStream outputStream, int headIndex, String command) throws Exception {
        List<String> logs = Logger.getLogs();
        StringBuilder headers = getHeaders();
        outputStream.write(headers.toString().getBytes());
        for (String log : logs) {
            outputStream.write((log + endStr).getBytes());
        }
        outputStream.flush();
    }
}
