/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kiokle.telegrambot.bean;

import java.io.Serializable;
import ru.kiokle.telegrambot.enums.Messages;

/**
 *
 * @author me
 */
public class CallbackDataBean implements Serializable {

    public static final String PREFIX = "c_";
    private Messages message;
    private String order;
    private boolean add;
    private Long chatId;

    public CallbackDataBean() {
    }

    public CallbackDataBean(Messages message, String order, boolean add, Long chatId) {
        this.message = message;
        this.order = order;
        this.add = add;
        this.chatId = chatId;
    }

    public Messages getMessage() {
        return message;
    }

    public void setMessage(Messages message) {
        this.message = message;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public boolean isAdd() {
        return add;
    }

    public void setAdd(boolean add) {
        this.add = add;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    @Override
    public String toString() {
        return PREFIX + message.ordinal() + "_" + order + "_" + add + "_" + chatId;
    }

    public static CallbackDataBean fromString(String str) {
        String[] split = str.split("_");
        return new CallbackDataBean(Messages.values()[Integer.valueOf(split[1])], split[2], Boolean.valueOf(split[3]), Long.valueOf(split[4]));
    }
}
