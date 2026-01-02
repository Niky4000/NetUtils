package ru.kiokle.telegrambot.database;

public class NotificationBean {

    private Long chatId;
    private String message;

    public NotificationBean(Long chatId, String message) {
        this.chatId = chatId;
        this.message = message;
    }

    public Long getChatId() {
        return chatId;
    }

    public String getMessage() {
        return message;
    }
}
