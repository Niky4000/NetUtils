package ru.kiokle.simplehttpserver.handlers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.localArgs;
import ru.kiokle.simplehttpserver.utils.FileUtils;

public class SelfPathCommandHandler implements CommandHandler {

    @Override
    public void handle(ByteArrayOutputStream byteArrayOutputStream, BufferedInputStream inputStream, int length, BufferedOutputStream outputStream, int headIndex, String command) throws Exception {
        StringBuilder headers = getHeaders();
        String selfExecCommand = FileUtils.getLaunchArguments(localArgs, FileUtils.getPathToJar().toPath());
        headers.append(selfExecCommand);
        outputStream.write(headers.toString().getBytes());
        outputStream.flush();
    }
}
