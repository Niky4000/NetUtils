package ru.kiokle.simplehttpserver.handlers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import ru.kiokle.simplehttpserver.utils.FileUtils;

public class Md5CommandHandler implements CommandHandler {

    @Override
    public void handle(ByteArrayOutputStream byteArrayOutputStream, BufferedInputStream inputStream, int length, BufferedOutputStream outputStream, int headIndex, String command) throws Exception {
        StringBuilder headers = getHeaders();
        String md5Sum = FileUtils.getMd5Sum(new File(command));
        headers.append(md5Sum);
        outputStream.write(headers.toString().getBytes());
        outputStream.flush();
    }
}
