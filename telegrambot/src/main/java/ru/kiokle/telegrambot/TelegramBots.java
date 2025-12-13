/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kiokle.telegrambot;

//import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 *
 * @author me
 */
public class TelegramBots {

    public static void start(String[] args) {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
//            telegramBotsApi.registerBot(new BotApi20(args[0]));
            telegramBotsApi.registerBot(new Bot(args[0]));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        String hello = "";
    }
//    public static SendMessage sendInlineKeyBoardMessage(long chatId) {
//        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
//        InlineKeyboardButton inlineKeyboardButton1 = new InlineKeyboardButton();
//        InlineKeyboardButton inlineKeyboardButton2 = new InlineKeyboardButton();
//        inlineKeyboardButton1.setText("Тык");
//        inlineKeyboardButton1.setCallbackData("Button \"Тык\" has been pressed");
//        inlineKeyboardButton2.setText("Тык2");
//        inlineKeyboardButton2.setCallbackData("Button \"Тык2\" has been pressed");
//        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
//        List<InlineKeyboardButton> keyboardButtonsRow2 = new ArrayList<>();
//        keyboardButtonsRow1.add(inlineKeyboardButton1);
//        keyboardButtonsRow1.add(new InlineKeyboardButton().setText("Fi4a").setCallbackData("CallFi4a"));
//        keyboardButtonsRow2.add(inlineKeyboardButton2);
//        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
//        rowList.add(keyboardButtonsRow1);
//        rowList.add(keyboardButtonsRow2);
//        inlineKeyboardMarkup.setKeyboard(rowList);
//        return new SendMessage().setChatId(chatId).setText("Пример").setReplyMarkup(inlineKeyboardMarkup);
//    }
}
