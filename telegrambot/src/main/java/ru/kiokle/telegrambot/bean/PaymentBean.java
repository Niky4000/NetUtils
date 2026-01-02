package ru.kiokle.telegrambot.bean;

public class PaymentBean {

    private Long id;
    private String idempotenceKey;
    private final String response;
    private final String paymentId;
    private final String confirmationUrl;
    private final String description;
    private final String status;

    public PaymentBean(String response, String idempotenceKey) {
        this.response = response;
        this.idempotenceKey = idempotenceKey;
        this.paymentId = getValue("id");
        this.confirmationUrl = getValue("confirmation_url");
        this.description = getValue("description");
        this.status = getValue("status");
    }

    private String getValue(String key) {
        String strKey = "\"" + key + "\" : \"";
        int index1 = response.indexOf(strKey);
        int index2 = response.indexOf("\"", index1 + strKey.length());
        return response.substring(index1 + strKey.length(), index2);
    }

    public Long getId() {
        return id;
    }

    public PaymentBean setId(Long id) {
        this.id = id;
        return this;
    }

    public String getIdempotenceKey() {
        return idempotenceKey;
    }

    public String getResponse() {
        return response;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getConfirmationUrl() {
        return confirmationUrl;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }
}
