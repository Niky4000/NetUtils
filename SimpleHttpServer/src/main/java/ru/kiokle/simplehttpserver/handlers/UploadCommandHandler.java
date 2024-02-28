package ru.kiokle.simplehttpserver.handlers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.BUFFER_SIZE;
import static ru.kiokle.simplehttpserver.handlers.CommandHandler.makeStandartOutput;

public class UploadCommandHandler implements CommandHandler {

    @Override
    public void handle(ByteArrayOutputStream byteArrayOutputStream, BufferedInputStream inputStream, int length, BufferedOutputStream outputStream, int headIndex, String command) {
        try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(new File(command)))) {
            bufferedOutputStream.write(byteArrayOutputStream.toByteArray(), headIndex, byteArrayOutputStream.size() - headIndex);
            byte[] buffer = new byte[BUFFER_SIZE];
            int read = 0;
            while (length > (byteArrayOutputStream.size() - headIndex) + read) {
                int read2 = inputStream.read(buffer);
                bufferedOutputStream.write(buffer, 0, read2);
                read += read2;
            }
            bufferedOutputStream.flush();
            makeStandartOutput(outputStream);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
