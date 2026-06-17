/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kiokle.breadsite;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.AbstractMap;
import java.util.Map;
import static ru.kiokle.breadsite.CommandHandler.makeStandartOutput;

/**
 *
 * @author me
 */
public class BreadSiteStart {

    public static void main(String[] args) throws IOException {
        new BreadSiteStart().startHttpServer(8080);
//        new DownloadExternalResources().download("https://duslyk-halal.ru/", new File("/home/me/Булки/duslyk/bread63/index.html"));
    }
    private static volatile boolean stop = false;
    private static volatile Integer port;

    public void startHttpServer(Integer port) throws IOException {
        this.port = port;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                if (stop) {
                    break;
                }
                new Thread(() -> {
                    try {
                        handleSocket(socket);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }
    }

    public static final int BUFFER_SIZE = 1024 * 10;

    private void handleSocket(Socket socket) throws Exception {
        try (BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE);
                BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE);) {
            readInputStream(inputStream, outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }
    public static final String startOfStream = "KIOKLE: KIOKLE";
    public final static byte[] endOfStream = new byte[]{0, 0, 10, 10, 10, 10, 0, 0};
    public static final String endStr = "\n";
    public static final String endStr2 = "\r";
    public static final byte[] end = endStr.getBytes();
    public static final String headEndStr = "\n\n";
    public static final String headEndStr2 = "\r\n\r\n";
    public static final byte[] headEnd = headEndStr.getBytes();
    public static final String delimiter = " ";

    private void readInputStream(BufferedInputStream inputStream, BufferedOutputStream outputStream) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int read = 0;
        do {
            byte[] buffer = new byte[BUFFER_SIZE];
            int startOfStreamContains = -1;
            int endOfStreamContains = -1;
            do {
                read = inputStream.read(buffer);
                if (read < 0 && !stop) {
                    break;
                }
                byteArrayOutputStream.write(buffer, 0, read);
                startOfStreamContains = contains(byteArrayOutputStream.toByteArray(), startOfStream.getBytes(), 0);
                if (startOfStreamContains > 0) {
                    endOfStreamContains = contains(byteArrayOutputStream.toByteArray(), endOfStream, startOfStreamContains);
                }
            } while (read > 0 && (startOfStreamContains > 0 && endOfStreamContains == -1));
            Map.Entry<String, Integer> headEntry = getHead(byteArrayOutputStream.toByteArray());
            if (headEntry == null) {
                String request = new String(byteArrayOutputStream.toByteArray());
                Logger.log(getHttpMethod(request));
                makeStandartOutput(outputStream, request);
                break;
            }
        } while (read > 0);
    }

    private String getHttpMethod(String request) {
        try {
            return request.substring(0, request.indexOf(endStr));
        } catch (Exception e) {
            return request;
        }
    }

    private Map.Entry<String, Integer> getHead(byte[] input) {
        return substringAfter(input, 0, headEnd);
    }

    private Map.Entry<String, Integer> substringAfter(byte[] bytes, int startIndex, byte[] end) {
        int index = contains(bytes, end, startIndex);
        if (index > -1) {
            return new AbstractMap.SimpleEntry<>(new String(bytes, startIndex, index), index + end.length);
        }
        return null;
    }

    private int contains(byte[] bytes, byte[] array, int startIndex) {
        for (int i = startIndex; i < bytes.length; i++) {
            boolean equal = true;
            for (int j = 0; j < array.length; j++) {
                if (i + j >= bytes.length || bytes[i + j] != array[j]) {
                    equal = false;
                    break;
                }
            }
            if (equal) {
                return i;
            }
        }
        return -1;
    }
}
