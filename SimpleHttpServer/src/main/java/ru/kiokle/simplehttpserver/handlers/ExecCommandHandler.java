package ru.kiokle.simplehttpserver.handlers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.endStr;

public class ExecCommandHandler implements CommandHandler {

    @Override
    public void handle(ByteArrayOutputStream byteArrayOutputStream, BufferedInputStream inputStream, int length, BufferedOutputStream outputStream, int headIndex, String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            try (BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String headers = "HTTP/1.1 200\n"
                        //                + "content-length: " + data.length + "\n"
                        + "cache-control: no-cache\n"
                        + "content-type: text/html\n"
                        + "connection: close\n\n";
                outputStream.write(headers.getBytes());
                String line;
                while ((line = input.readLine()) != null) {
                    outputStream.write((line + endStr).getBytes());
                }
                outputStream.flush();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
