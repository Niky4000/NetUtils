package ru.kiokle.simplehttpserver.log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Logger {

    private static Deque<String> logList = new LinkedList<>();
    public static final int MAX_LOG_SIZE = 256;

    public static void log(String message) {
        String logMessage = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss_SSS").format(new Date()) + ": " + message;
        System.out.println(logMessage);
        synchronized (Logger.class) {
            logList.addFirst(logMessage);
            if (logList.size() > MAX_LOG_SIZE) {
                logList.pollLast();
            }
        }
    }

    public static synchronized List<String> getLogs(int count) {
        List<String> arrayList = new ArrayList<>();
        Iterator<String> iterator = logList.iterator();
        int i = 0;
        while (iterator.hasNext() && i++ < count) {
            arrayList.add(iterator.next());
        }
        return arrayList;
    }
}
