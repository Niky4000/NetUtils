package ru.kiokle.simplehttpserver.handlers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import ru.kiokle.simplehttpserver.utils.FileUtils;

public class SelfPathCommandHandler implements CommandHandler {

    @Override
    public void handle(ByteArrayOutputStream byteArrayOutputStream, BufferedInputStream inputStream, int length, BufferedOutputStream outputStream, int headIndex, String command) throws Exception {
        StringBuilder headers = getHeaders();
        File pathToJar = FileUtils.getPathToJar();
        headers.append(pathToJar.getAbsolutePath());
        outputStream.write(headers.toString().getBytes());
        outputStream.flush();
    }
}
