package ru.kiokle.simplehttpserver.handlers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.BUFFER_SIZE;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.endOfStream;

public class UploadCommandHandler implements CommandHandler {

    @Override
    public void handle(ByteArrayOutputStream byteArrayOutputStream, BufferedInputStream inputStream, int length, BufferedOutputStream outputStream, int headIndex, String command) throws Exception {
        try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(new File(command)))) {
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            int offset = headIndex + endOfStream.length;
            long writtenBytes = length > byteArray.length - offset ? byteArray.length - offset : length;
            long estimatedBytes = length - writtenBytes;
            bufferedOutputStream.write(byteArray, offset, (int) writtenBytes);
            byte[] buffer = new byte[BUFFER_SIZE];
            long read = 0;
            while (length > (byteArrayOutputStream.size() - headIndex) + read) {
                int read2 = inputStream.read(buffer);
                bufferedOutputStream.write(buffer, 0, estimatedBytes > read2 ? read2 : (int) estimatedBytes);
                read += read2;
                estimatedBytes -= read2;
            }
            bufferedOutputStream.flush();
            StringBuilder headers = getHeaders();
            headers.append(command + " was uploaded!");
            outputStream.write(headers.toString().getBytes());
            outputStream.flush();
        }
    }
}
