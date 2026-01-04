package ru.kiokle.telegrambot.logs;

import java.util.Date;

public class LogBean {

    private String log;
    private Date created;

    public LogBean(String log) {
        this.log = log;
        this.created = new Date();
    }

    public String getLog() {
        return log;
    }

    public Date getCreated() {
        return created;
    }
}
