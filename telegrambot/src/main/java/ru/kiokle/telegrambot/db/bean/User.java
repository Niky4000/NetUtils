package ru.kiokle.telegrambot.db.bean;

public class User {

    private Long id;
    private String login;
    private Long chatId;

    public User(Long id, String login) {
        this.id = id;
        this.login = login;
    }

    public User(Long id, String login, Long chatId) {
        this.id = id;
        this.login = login;
        this.chatId = chatId;
    }

    public Long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public Long getChatId() {
        return chatId;
    }
}
