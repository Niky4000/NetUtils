package ru.kiokle.telegrambot.logs;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class LogsTest {

    @Test
    public void testLogs() {
        int LOG_SIZE = 12;
        Logs logs = new Logs(LOG_SIZE);
        logs.log(new RuntimeException());
        logs.log(new RuntimeException());
        logs.log(new RuntimeException());
        logs.log(new RuntimeException());
        List<LogBean> exceptionList = logs.getExceptionList();
        Assert.assertTrue(exceptionList.size() == 12);
    }
}
