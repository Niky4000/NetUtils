package ru.kiokle.simplehttpserver.clients;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.BUFFER_SIZE;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.delimiter;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.endOfStream;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.endStr;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.headEndStr;
import ru.kiokle.simplehttpserver.clients.enums.ConnectionType;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.LENGTH;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.UPLOAD;
import ru.kiokle.simplehttpserver.log.Logger;

public class UploadClient extends Client {

    protected final File file;
    protected final String toFile;

    public UploadClient(List<String> argList, String destinationHost, int destinationPort, int proxyPort, ConnectionType connectionType, File file, String toFile) {
        super(argList, destinationHost, destinationPort, proxyPort, connectionType);
        this.file = file;
        this.toFile = toFile;
    }

    public static String createHead(String targetFile, long length) {
        return UPLOAD.name() + delimiter + targetFile + endStr + LENGTH.name() + delimiter + length + headEndStr;
    }

    @Override
    public void handle(BufferedOutputStream outputStream, BufferedInputStream inputStream) throws Exception {
        try (InputStream fileInputStream = new FileInputStream(file)) {
            sendFile(outputStream, inputStream, fileInputStream, file.length(), toFile);
        }
    }

    protected void sendFile(BufferedOutputStream outputStream, BufferedInputStream inputStream, InputStream fileInputStream, long fileLength, String toFile) throws IOException {
        outputStream.write(makeHeadBytes(() -> createHead(toFile, fileLength)));
        outputStream.write(endOfStream);
        for (int i = 0; i < getIterationCount(fileLength); i++) {
            byte[] buffer = new byte[BUFFER_SIZE];
            fileInputStream.read(buffer);
            outputStream.write(buffer);
        }
        outputStream.flush();
        String readInputStream = readInputStream(inputStream);
        Logger.log(readInputStream);
    }

    private int getIterationCount(long fileLength) {
        if (fileLength < BUFFER_SIZE) {
            return 1;
        } else if (fileLength % BUFFER_SIZE != 0) {
            return ((int) (fileLength / BUFFER_SIZE)) + 1;
        } else {
            return (int) (fileLength / BUFFER_SIZE);
        }
    }
}
