package ru.kiokle.simplehttpserver.handlers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public interface CommandHandler {

    public void handle(ByteArrayOutputStream byteArrayOutputStream, BufferedInputStream inputStream, int length, BufferedOutputStream outputStream, int headIndex, String command) throws Exception;

    public static void makeStandartOutput(final BufferedOutputStream outputStream) throws IOException {
        byte[] data = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " Hello World!!!").getBytes();
        String headers = "HTTP/1.1 200\n"
                + "content-length: " + data.length + "\n"
                + "cache-control: no-cache\n"
                + "content-type: text/html\n"
                + "connection: close\n\n";
        outputStream.write(headers.getBytes());
        outputStream.write(data);
        outputStream.flush();
    }

    default StringBuilder getHeaders() {
        return new StringBuilder("HTTP/1.1 200\n"
                + "cache-control: no-cache\n"
                + "content-type: text/html\n"
                + "connection: close\n\n");
    }
}
