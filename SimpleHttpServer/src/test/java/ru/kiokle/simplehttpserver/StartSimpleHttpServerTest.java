package ru.kiokle.simplehttpserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import org.junit.Assert;
import org.junit.Test;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.BUFFER_SIZE;
import static ru.kiokle.simplehttpserver.clients.ExecCommandClient.createCommand;
import static ru.kiokle.simplehttpserver.clients.UploadClient.createHead;
import ru.kiokle.simplehttpserver.log.Logger;
import static ru.kiokle.simplehttpserver.utils.FileUtils.LOCAL_BUFFER_SIZE;

public class StartSimpleHttpServerTest {

    private static final int PORT = 32534;
    private static final int TIME_TO_WAIT_FOR_SERVER_TO_START = 200;

    @Test
    public void handleSocketTest() throws Exception {
        String baseDir = FileUtils.getPathToJar().getParentFile().getAbsolutePath();
        String testFile = "test_file.txt";
        String fullTestPath = baseDir + File.separator + testFile;
        long testFileLength = ru.kiokle.simplehttpserver.utils.FileUtils.readAllBytesFromResource(StartSimpleHttpServerTest.class, "test/upload/file/test_file.txt").length;
        long targetFileLength = (BUFFER_SIZE / 8) * testFileLength;
        try {
            final StartSimpleHttpServer simpleHttpServer = new StartSimpleHttpServer();
            Thread listerner = createListerner(simpleHttpServer);
            try (Socket socket = new Socket("127.0.0.1", PORT);
                    BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream(), LOCAL_BUFFER_SIZE);
                    BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream(), LOCAL_BUFFER_SIZE);) {
                outputStream.write(createHead(fullTestPath, targetFileLength).getBytes());
                for (int i = 0; i < BUFFER_SIZE / 8; i++) {
                    outputStream.write(ru.kiokle.simplehttpserver.utils.FileUtils.readAllBytesFromResource(StartSimpleHttpServerTest.class, "test/upload/file/test_file.txt"));
                }
                outputStream.flush();
                byte[] readInputStream = readInputStream(inputStream);
                Logger.log(new String(readInputStream));
            }
            simpleHttpServer.stopHttpServer();
            listerner.join();
            Assert.assertTrue(new File(fullTestPath).exists());
            Assert.assertTrue(Long.compare(new File(fullTestPath).length(), targetFileLength) == 0);
        } finally {
            File file = new File(fullTestPath);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    @Test
    public void execCommandTest() throws Exception {
        String baseDir = FileUtils.getPathToJar().getParentFile().getAbsolutePath();
        String input = null;
        final StartSimpleHttpServer simpleHttpServer = new StartSimpleHttpServer();
        Thread listerner = createListerner(simpleHttpServer);
        try (Socket socket = new Socket("127.0.0.1", PORT);
                BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream(), LOCAL_BUFFER_SIZE);
                BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream(), LOCAL_BUFFER_SIZE);) {
            outputStream.write(createCommand("ls -la " + baseDir).getBytes());
            outputStream.flush();
            byte[] readInputStream = readInputStream(inputStream);
            if (readInputStream.length > 0) {
                input = new String(readInputStream);
            }
            Logger.log(input);
        }
        simpleHttpServer.stopHttpServer();
        listerner.join();
        Assert.assertTrue(input != null && input.length() > 0);
    }

    private Thread createListerner(final StartSimpleHttpServer simpleHttpServer) throws InterruptedException {
        Thread listerner = new Thread(() -> {
            try {
                simpleHttpServer.startHttpServer(PORT);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        listerner.setName("Socket Listerner");
        listerner.start();
        listerner.join(TIME_TO_WAIT_FOR_SERVER_TO_START);
        return listerner;
    }

    private byte[] readInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int read = 0;
        do {
            byte[] buffer = new byte[LOCAL_BUFFER_SIZE];
            read = inputStream.read(buffer);
            byteArrayOutputStream.write(buffer, 0, read);
            if (read < LOCAL_BUFFER_SIZE) {
                break;
            }
        } while (read > 0);
        return byteArrayOutputStream.toByteArray();
    }

}
