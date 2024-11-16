package ru.kiokle.services.restart;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StartRestartServices {

    static long TIME_TO_WAIT_BEFORE_START = 1000 * 60 * 2;
    static long TIME_TO_WAIT_AFTER_START = 1000 * 60 * 60 * 4;

    public static void main(String[] args) throws Exception {
        while (true) {
            exec(new ByteArrayOutputStream(), "sc stop i2p");
            System.out.println("Stopped! " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "!");
            waitSomeTime(TIME_TO_WAIT_BEFORE_START);
            exec(new ByteArrayOutputStream(), "sc start i2p");
            System.out.println("Started! " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "!");
            waitSomeTime(TIME_TO_WAIT_AFTER_START);
        }
    }

    static void waitSomeTime(long time) {
        try {
            Thread.sleep(time);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void exec(OutputStream outputStream, String command) throws Exception {
        Process process = Runtime.getRuntime().exec(command);
        try (BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = input.readLine()) != null) {
                outputStream.write((line).getBytes());
            }
            outputStream.flush();
        }
    }
}
