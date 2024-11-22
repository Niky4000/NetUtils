package ru.kiokle.simplehttpserver.handlers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.endStr;

public class ExecCommandHandler implements CommandHandler {

    private static String async = " &";

    @Override
    public void handle(ByteArrayOutputStream byteArrayOutputStream, BufferedInputStream inputStream, int length, BufferedOutputStream outputStream, int headIndex, String command) throws Exception {
        String commandCmd;
        boolean isAsync = false;
        if (command.endsWith(async)) {
            commandCmd = command.substring(0, command.lastIndexOf(async));
            isAsync = true;
        } else {
            commandCmd = command;
        }
        Process process = Runtime.getRuntime().exec(commandCmd);
        if (!isAsync) {
            try (BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder headers = getHeaders();
                outputStream.write(headers.toString().getBytes());
                String line;
                while ((line = input.readLine()) != null) {
                    outputStream.write((line + endStr).getBytes());
                }
                outputStream.flush();
            }
        }
    }
}
