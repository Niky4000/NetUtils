package ru.kiokle.telegrambot.db.bean;

import java.util.Date;

public class Order {

    long id;
    long userId;
    long orderId;
    String name;
    long price;
    int quantity;
    Date created;
    boolean finished;
    boolean active;

    public Order(long userId, String name, long price, int quantity) {
        this.userId = userId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.created = new Date();
        this.finished = false;
        this.active = true;
    }

    public Order(long id, long userId, String name, long price, int quantity, Date created, boolean finished, boolean active) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.created = created;
        this.finished = finished;
        this.active = active;
    }

    public Order update(Order order) {
        this.name = order.getName();
        this.price = order.getPrice();
        this.quantity = order.getQuantity();
        this.finished = order.isFinished();
        this.active = order.isActive();
        return this;
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public Order setOrderId(long orderId) {
        this.orderId = orderId;
        return this;
    }

    public long getOrderId() {
        return orderId;
    }

    public String getName() {
        return name;
    }

    public long getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public Date getCreated() {
        return created;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
