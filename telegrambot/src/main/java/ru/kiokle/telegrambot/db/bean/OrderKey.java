/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kiokle.telegrambot.db.bean;

import java.util.Date;
import java.util.Objects;

/**
 *
 * @author me
 */
public class OrderKey {

    private String name;
    private Date created;

    public OrderKey(String name) {
        this.name = name;
        this.created = new Date();
    }

    public String getName() {
        return name;
    }

    public Date getCreated() {
        return created;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.name);
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
        final OrderKey other = (OrderKey) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }
}
