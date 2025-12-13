/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.some.tcp;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author me
 */
public class Logger {

    private static boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
    private static volatile boolean enabled = false;
    private static List<String> messages = new ArrayList<>();
    private static int MAX_SIZE = 64;

    public static synchronized void log(String message) {
        log(message, null);
    }

    public static synchronized void log(Exception e) {
        log(null, e);
    }

    public static synchronized void log(String message, Exception e) {
        if (message != null) {
            if (isWindows || enabled) {
                System.out.println(message);
            }
            addMessage(message);
        }
        if (e != null) {
            if (isWindows || enabled) {
                e.printStackTrace();
            }
            StackTraceElement[] stackTraceArray = e.getStackTrace();
            for (StackTraceElement element : stackTraceArray) {
                addMessage(element.toString());
            }
        }

    }

    private static synchronized void addMessage(String message) {
        messages.add(message);
        if (messages.size() > MAX_SIZE) {
            messages.remove(messages.size() - 1);
        }
    }

    public static synchronized List<String> getMessages() {
        List<String> list = new ArrayList<>(messages);
        messages.clear();
        return list;
    }

    public static synchronized void enable() {
        enabled = true;
    }
}
