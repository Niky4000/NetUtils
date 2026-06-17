package ru.kiokle.simplehttpserver.clients;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.List;
import ru.kiokle.simplehttpserver.clients.enums.ConnectionType;

/**
 *
 * @author me
 */
public class UploadBigFilesClient extends UploadClient {

    private static final int chunkSize = 1024 * 1024;

    public UploadBigFilesClient(List<String> argList, String destinationHost, int destinationPort, int proxyPort, ConnectionType connectionType, File file, String toFile) {
        super(argList, destinationHost, destinationPort, proxyPort, connectionType, file, toFile);
    }

    @Override
    public void handle(BufferedOutputStream outputStream, BufferedInputStream inputStream) throws Exception {
        Long filePointer = 0L;
        long fileLength = file.length();
        long iterationCount = fileLength / chunkSize + (fileLength % chunkSize > 0L ? 1 : 0);
        for (long chunkNumber = 0; chunkNumber < iterationCount; chunkNumber++) {
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
                randomAccessFile.seek(filePointer);
                byte[] buffer = new byte[chunkSize];
                randomAccessFile.read(buffer);
                try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer)) {
                    sendFile(outputStream, inputStream, byteArrayInputStream, (long) byteArrayInputStream.available(), toFile + "~" + chunkNumber);
                }
            }
            filePointer += chunkSize;
        }
    }
}
