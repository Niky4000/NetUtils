package ru.kiokle.simplehttpserver.handlers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import ru.kiokle.simplehttpserver.StartSimpleHttpServer;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.TIME_TO_WAIT;
import ru.kiokle.simplehttpserver.log.Logger;
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
                Logger.log("Server was stopped!");
                WaitUtils.waitSomeTime(TIME_TO_WAIT);
                System.exit(0);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        thread.setName("stopThread");
        thread.start();
    }
}
