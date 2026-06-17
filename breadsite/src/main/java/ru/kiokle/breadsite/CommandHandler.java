package ru.kiokle.breadsite;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface CommandHandler {

    public void handle(ByteArrayOutputStream byteArrayOutputStream, BufferedInputStream inputStream, int length, BufferedOutputStream outputStream, int headIndex, String command) throws Exception;

    public static void makeStandartOutput(final BufferedOutputStream outputStream, String request) throws IOException {
        if (request.length() > 0) {
            HttpResponse data = new FileSystemHandler().getData(request);
            outputStream.write(data.getHeaders());
            outputStream.write(data.getData());
            outputStream.flush();
        }
    }

    default StringBuilder getHeaders() {
        return new StringBuilder("HTTP/1.1 200\n"
                + "cache-control: no-cache\n"
                + "content-type: text/html\n"
                + "connection: close\n\n");
    }
}
