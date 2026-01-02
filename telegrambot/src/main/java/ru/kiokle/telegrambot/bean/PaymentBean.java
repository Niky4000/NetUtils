package ru.kiokle.telegrambot.bean;

public class PaymentBean {

    private Long id;
    private String idempotenceKey;
    private final String response;
    private final String paymentId;
    private final String confirmationUrl;
    private final String description;
    private final String status;
    private final String amount;
    private final String incomeAmount;

    public PaymentBean(String response, String idempotenceKey) {
        this.response = response;
        this.idempotenceKey = idempotenceKey;
        this.paymentId = getValue("id", response);
        this.confirmationUrl = getValue("confirmation_url", response);
        this.description = getValue("description", response);
        this.status = getValue("status", response);
        this.amount = getNestedValue("amount", "value");
        this.incomeAmount = getNestedValue("income_amount", "value");
    }

    private String getValue(String key, String response) {
        String strKey = "\"" + key + "\" : \"";
        int index1 = response.indexOf(strKey);
        if (index1 < 0) {
            return null;
        }
        int index2 = response.indexOf("\"", index1 + strKey.length());
        return response.substring(index1 + strKey.length(), index2);
    }

    private String getNestedValue(String key, String key2) {
        String strKey = "\"" + key + "\" : {";
        int index1 = response.indexOf(strKey);
        if (index1 < 0) {
            return null;
        }
        int index2 = response.indexOf("}", index1 + strKey.length());
        String substring = response.substring(index1 + strKey.length(), index2);
        String value = getValue(key2, substring);
        return value;
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

    public String getAmount() {
        return amount;
    }

    public String getIncomeAmount() {
        return incomeAmount;
    }
}
