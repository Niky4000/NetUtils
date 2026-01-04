package ru.kiokle.telegrambot.db.bean;

import java.util.Date;

public class UserOrder {

    long id;
    long userId;
    Date created;
    long price;
    boolean finished;
    boolean paid;
    boolean active;

    public UserOrder(long userId) {
        this.userId = userId;
        this.created = new Date();
        this.finished = false;
        this.active = true;
    }

    public UserOrder(long id, long userId, Date created, long price, boolean finished, boolean paid, boolean active) {
        this.id = id;
        this.userId = userId;
        this.created = created;
        this.price = price;
        this.finished = finished;
        this.paid = paid;
        this.active = active;
    }

    public UserOrder update(UserOrder userOrder) {
        this.price = userOrder.getPrice();
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

    public boolean isPaid() {
        return paid;
    }

    public boolean isActive() {
        return active;
    }

    public UserOrder setFinished(boolean finished) {
        this.finished = finished;
        return this;
    }

    public UserOrder setPaid(boolean paid) {
        this.paid = paid;
        return this;
    }

    public UserOrder setActive(boolean active) {
        this.active = active;
        return this;
    }

}
