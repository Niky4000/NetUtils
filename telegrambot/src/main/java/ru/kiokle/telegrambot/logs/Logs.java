package ru.kiokle.telegrambot.logs;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 *
 * @author me
 */
public class Logs {

    private final LinkedList<LogBean> exceptionList = new LinkedList<>();
    private final int MAX_LOGS_LENGTH;

    public Logs(int MAX_LOGS_LENGTH) {
        this.MAX_LOGS_LENGTH = MAX_LOGS_LENGTH;
    }

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
        synchronized (exceptionList) {
            exceptionList.addAll(split);
            if (exceptionList.size() > MAX_LOGS_LENGTH) {
                int iterations = exceptionList.size() - MAX_LOGS_LENGTH;
                Iterator<LogBean> descendingIterator = exceptionList.descendingIterator();
                while (descendingIterator.hasNext() && iterations > 0) {
                    descendingIterator.next();
                    descendingIterator.remove();
                    iterations--;
                }
            }
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
        synchronized (exceptionList) {
            exceptionList.removeIf(u -> {
                return u.getCreated().before(yesterday);
            });
        }
    }

    public List<LogBean> getExceptionList() {
        return new ArrayList<>(exceptionList);
    }
}
