package ru.kiokle.telegrambot.bean;

public class OrderBean {

    private String pathToPicture;
    private String description;
    private Long price;
    private String currency;
    private String type;
    private String name;

    public OrderBean() {
    }

    public OrderBean(String pathToPicture, String description, Long price, String currency, String type, String name) {
        this.pathToPicture = pathToPicture;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.type = type;
        this.name = name;
    }

    public String getPathToPicture() {
        return pathToPicture;
    }

    public void setPathToPicture(String pathToPicture) {
        this.pathToPicture = pathToPicture;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
