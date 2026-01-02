package ru.kiokle.telegrambot.db.bean;

import java.util.Date;

public class Payment {

    long id;
    long orderId;
    String paymentId;
    String idempotenceKey;
    String description;
    long price;
    Date created;
    boolean active;
    String status;
    String response;

    public Payment(long id, long orderId, String paymentId, String idempotenceKey, String description, long price, Date created, boolean active, String status, String response) {
        this.id = id;
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.idempotenceKey = idempotenceKey;
        this.description = description;
        this.price = price;
        this.created = created;
        this.active = active;
        this.status = status;
        this.response = response;
    }

    public long getId() {
        return id;
    }

    public long getOrderId() {
        return orderId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getIdempotenceKey() {
        return idempotenceKey;
    }

    public String getDescription() {
        return description;
    }

    public long getPrice() {
        return price;
    }

    public Date getCreated() {
        return created;
    }

    public boolean isActive() {
        return active;
    }

    public String getStatus() {
        return status;
    }

    public String getResponse() {
        return response;
    }
}
