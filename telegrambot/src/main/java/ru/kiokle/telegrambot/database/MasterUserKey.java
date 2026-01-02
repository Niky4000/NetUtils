package ru.kiokle.telegrambot.database;

import java.util.Date;
import java.util.Objects;

public class MasterUserKey {

    private Long id;
    private String username;
    private Long chatId;
    private Date created;

    public MasterUserKey() {
        this.id = -1L;
    }

    public MasterUserKey(String username) {
        this.username = username;
        this.created = new Date();
    }

    public MasterUserKey(Long id, String username, Long chatId) {
        this.id = id;
        this.username = username;
        this.chatId = chatId;
        this.created = new Date();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Long getChatId() {
        return chatId;
    }

    public Date getCreated() {
        return created;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.username);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MasterUserKey other = (MasterUserKey) obj;
        if (!Objects.equals(this.username, other.username)) {
            return false;
        }
        return true;
    }
}
