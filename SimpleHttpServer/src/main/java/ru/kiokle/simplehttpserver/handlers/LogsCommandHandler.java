package ru.kiokle.simplehttpserver.handlers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.endStr;
import ru.kiokle.simplehttpserver.log.Logger;
import static ru.kiokle.simplehttpserver.log.Logger.MAX_LOG_SIZE;

public class LogsCommandHandler implements CommandHandler {

    @Override
    public void handle(ByteArrayOutputStream byteArrayOutputStream, BufferedInputStream inputStream, int length, BufferedOutputStream outputStream, int headIndex, String command) throws Exception {
        List<String> logs = Logger.getLogs(getCount(command));
        StringBuilder headers = getHeaders();
        outputStream.write(headers.toString().getBytes());
        for (String log : logs) {
            outputStream.write((log + endStr).getBytes());
        }
        outputStream.flush();
    }

    private int getCount(String command) {
        try {
            return Integer.valueOf(command);
        } catch (Exception e) {
            return MAX_LOG_SIZE;
        }
    }
}
