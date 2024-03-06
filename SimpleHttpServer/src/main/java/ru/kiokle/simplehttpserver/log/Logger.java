package ru.kiokle.simplehttpserver.log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class Logger {

    private static Deque<String> logList = new LinkedList<>();
    private static final int MAX_LOG_SIZE = 2048;

    public static void log(String message) {
        String logMessage = new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ": " + message;
        System.out.println(logMessage);
        synchronized (Logger.class) {
            logList.addFirst(logMessage);
            if (logList.size() > MAX_LOG_SIZE) {
                logList.pollLast();
            }
        }
    }

    public static synchronized List<String> getLogs() {
        return new ArrayList<>(logList);
    }
}
