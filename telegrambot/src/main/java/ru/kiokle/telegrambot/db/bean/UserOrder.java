package ru.kiokle.telegrambot.db.bean;

import java.util.Date;

public class UserOrder {

    long id;
    long userId;
    Date created;
    long price;
    boolean finished;
    boolean active;

    public UserOrder(long userId) {
        this.userId = userId;
        this.created = new Date();
        this.finished = false;
        this.active = true;
    }

    public UserOrder(long id, long userId, Date created, long price, boolean finished, boolean active) {
        this.id = id;
        this.userId = userId;
        this.created = created;
        this.price = price;
        this.finished = finished;
        this.active = active;
    }

    public UserOrder update(UserOrder userOrder) {
        this.finished = userOrder.isFinished();
        this.active = userOrder.isActive();
        return this;
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public Date getCreated() {
        return created;
    }

    public long getPrice() {
        return price;
    }

    public UserOrder setPrice(long price) {
        this.price = price;
        return this;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isActive() {
        return active;
    }

    public UserOrder setActive(boolean active) {
        this.active = active;
        return this;
    }
}
