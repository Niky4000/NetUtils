/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kiokle.telegrambot.utils;

import ru.kiokle.telegrambot.bean.CallbackDataBean;
import ru.kiokle.telegrambot.bean.CallbackOrderRecievedBean;
import ru.kiokle.telegrambot.enums.Messages;

/**
 *
 * @author me
 */
public class MessageUtils {

    public static Messages fromString(String str) {
        if (str.startsWith(CallbackDataBean.PREFIX)) {
            return CallbackDataBean.fromString(str).getMessage();
        } else if (str.startsWith(CallbackOrderRecievedBean.PREFIX)) {
            return CallbackOrderRecievedBean.fromString(str).getMessage();
        } else {
            return null;
        }
    }
}
