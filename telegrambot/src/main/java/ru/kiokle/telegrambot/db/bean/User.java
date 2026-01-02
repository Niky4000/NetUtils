package ru.kiokle.telegrambot.db.bean;

public class User {

    private Long id;
    private String login;

    public User(Long id, String login) {
        this.id = id;
        this.login = login;
    }

    public Long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }
}
