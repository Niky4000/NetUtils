package ru.kiokle.telegrambot.logs;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

/**
 *
 * @author me
 */
public class Logs {

    private final List<LogBean> eceptionList = new ArrayList<>();

    public void init() {
        Thread logsCleaningThread = new Thread(() -> {
            cleanLogs();
            waitSomeTime(60);
        }, "logsCleaningThread");
        logsCleaningThread.setDaemon(true);
        logsCleaningThread.start();
    }

    public void log(Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        List<LogBean> split = Stream.of(sw.toString().split("\n")).map(LogBean::new).toList();
        synchronized (eceptionList) {
            eceptionList.addAll(split);
        }
    }

    private void waitSomeTime(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ex) {
        }
    }

    private void cleanLogs() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        Date yesterday = calendar.getTime();
        synchronized (eceptionList) {
            eceptionList.removeIf(u -> {
                return u.getCreated().before(yesterday);
            });
        }
    }

}
