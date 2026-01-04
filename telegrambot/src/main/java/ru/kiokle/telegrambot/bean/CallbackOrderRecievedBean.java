/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kiokle.telegrambot.bean;

import ru.kiokle.telegrambot.enums.Messages;

/**
 *
 * @author me
 */
public class CallbackOrderRecievedBean {

    public static final String PREFIX = "m_";
    private Messages message;
    private Long chatId;
    private Long userChatId;
    private Long orderId;

    public CallbackOrderRecievedBean() {
    }

    public CallbackOrderRecievedBean(Messages message, Long chatId, Long userChatId, Long orderId) {
        this.message = message;
        this.chatId = chatId;
        this.userChatId = userChatId;
        this.orderId = orderId;
    }

    public Messages getMessage() {
        return message;
    }

    public void setMessage(Messages message) {
        this.message = message;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Long getUserChatId() {
        return userChatId;
    }

    public void setUserChatId(Long userChatId) {
        this.userChatId = userChatId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    @Override
    public String toString() {
        return PREFIX + message.ordinal() + "_" + chatId + "_" + userChatId + "_" + orderId;
    }

    public static CallbackOrderRecievedBean fromString(String str) {
        String[] split = str.split("_");
        return new CallbackOrderRecievedBean(Messages.values()[Integer.valueOf(split[1])], Long.valueOf(split[2]), Long.valueOf(split[3]), Long.valueOf(split[4]));
    }
}
