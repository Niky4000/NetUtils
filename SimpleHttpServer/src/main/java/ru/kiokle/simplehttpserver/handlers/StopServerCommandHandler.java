package ru.kiokle.simplehttpserver.handlers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ru.kiokle.simplehttpserver.StartSimpleHttpServer;
import ru.kiokle.simplehttpserver.utils.WaitUtils;

public class StopServerCommandHandler implements CommandHandler {

    @Override
    public void handle(ByteArrayOutputStream byteArrayOutputStream, BufferedInputStream inputStream, int length, BufferedOutputStream outputStream, int headIndex, String command) throws Exception {
        StringBuilder headers = getHeaders();
        headers.append("Stopped!");
        outputStream.write(headers.toString().getBytes());
        outputStream.flush();
        creteStopThread();
    }

    private void creteStopThread() {
        Thread thread = new Thread(() -> {
            WaitUtils.waitSomeTime(2);
            try {
                StartSimpleHttpServer.stopHttpServer();
                System.out.println("Server was stopped!");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        thread.setName("stopThread");
        thread.start();
    }
}
