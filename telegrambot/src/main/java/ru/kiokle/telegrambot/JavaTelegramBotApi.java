/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kiokle.telegrambot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.InputMediaPhoto;
import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMediaGroup;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author me
 */
public class JavaTelegramBotApi {

    private static final String COOKIES = "cookies";
    private static final String BREAD = "bread";
    private static final String ORDER = "order";
    private static final Map<String, String> map = Map.of(COOKIES, "печенька", BREAD, "булочка");

    private static final ConcurrentHashMap<Long, ConcurrentHashMap<String, AtomicInteger>> orders = new ConcurrentHashMap<>();

    public static void start(String[] args) {
        if (args.length > 0) {
            String token = args[0];
// Create your bot passing the token received from @BotFather
            TelegramBot bot = new TelegramBot(token);

// Register for updates
            bot.setUpdatesListener(updates -> {
                // ... process updates
                // return id of last processed update or confirm them all
                updates.forEach(update -> {
                    if (update.message() != null) {
                        long chatId = update.message().chat().id();
                        sendPhotoWithButton(bot, chatId, "/home/me/Булки/bread.jpg", "Булка классическая белая\n150 руб", BREAD);
                        sendPhotoWithButton(bot, chatId, "/home/me/Булки/cookies.png", "Овсяные печеньки с корицей\n50 руб", COOKIES);
                        sendOrderButton(bot, chatId);
//                    SendResponse response = bot.execute(new SendMessage(chatId, "Hello!"));
//                        sendPhotoWithButton(bot, chatId);
//                        sendFotos(bot, chatId);
//                        sendFotos2(bot, chatId);
                    } else if (update.callbackQuery() != null && update.callbackQuery().data() != null) {
                        CallbackDataBean callBackData = readCallBackData(update.callbackQuery().data());
                        Long chatId = callBackData.getChatId();
                        String callbackId = update.callbackQuery().id();
                        if (ORDER.equals(callBackData.getOrder())) {
                            createOrderTotal(bot, chatId, callbackId);
                        } else if (callBackData.isAdd()) {
                            createMessageSomethingWasAdded(bot, chatId, map.get(callBackData.getOrder()), callbackId);
                        } else if (!callBackData.isAdd()) {
                            createMessageSomethingWasRemoved(bot, chatId, map.get(callBackData.getOrder()), callbackId);
                        }
                    }
                });

                return UpdatesListener.CONFIRMED_UPDATES_ALL;
// Create Exception Handler
            }, e -> {
                if (e.response() != null) {
                    // got bad response from telegram
                    e.response().errorCode();
                    e.response().description();
                } else {
                    // probably network error
                    e.printStackTrace();
                }
            });
        }
    }

    public static void sendFotos2(TelegramBot bot, long chatId) {
        // 1. Create SendPhoto object
        SendPhoto sendPhoto = new SendPhoto(chatId, new File("/home/me/Булки/bread.jpg"));
//        sendPhoto.setChatId(String.valueOf(chatId));
//        sendPhoto.setPhoto(new InputFile(new File("path/to/your/image.jpg"))); // Or use file_id/URL
        sendPhoto.setCaption("Here's an image with a button!");

// 2. Create InlineKeyboardMarkup
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

// 3. Create InlineKeyboardButton(s)
        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText("Click Me!");
        button1.setCallbackData("button_clicked"); // Data sent to bot on click

// 4. Arrange buttons in rows
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(button1);
        keyboard.add(row1);

// 5. Add rows to the keyboard markup
//        inlineKeyboardMarkup.setKeyboard(keyboard);
// 6. Attach the keyboard to the photo
        sendPhoto.setReplyMarkup(inlineKeyboardMarkup);

// 7. Execute the request
        bot.execute(sendPhoto);
    }

    public static void sendFotos(TelegramBot bot, long chatId) {
        // Create InputMediaPhoto objects for each image
        InputMediaPhoto photo1 = new InputMediaPhoto(new File("/home/me/Булки/bread.jpg"));
        photo1.caption("First image from local file");

        InputMediaPhoto photo2 = new InputMediaPhoto(new File("/home/me/Булки/cookies.png"));
        photo2.caption("Second image from URL");

//        InputMediaPhoto photo3 = new InputMediaPhoto(new File("path/to/local/image3.png"));
//        photo3.caption("Third image with another caption");
        // Create a SendMediaGroup request with all photos
        SendMediaGroup sendMediaGroup = new SendMediaGroup(chatId, photo1, photo2);

        // Execute the request
        bot.execute(sendMediaGroup);
        System.out.println("Multiple images sent successfully!");
    }

    private static void sendPhotoWithButton(TelegramBot bot, long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton button = new InlineKeyboardButton("buttonText").callbackData("callbackData");
        InlineKeyboardButton button2 = new InlineKeyboardButton("buttonText2").callbackData("callbackData2");
        markup.addRow(button, button2);
        SendPhoto sendPhoto = new SendPhoto(chatId, new File("/home/me/Булки/bread.jpg")).caption("caption").replyMarkup(markup);
        bot.execute(sendPhoto);
    }

    private static void sendPhotoWithButton(TelegramBot bot, long chatId, String imagePath, String caption, String callbackData) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton button = new InlineKeyboardButton("Заказать").callbackData(callBackData(chatId, callbackData, true));
        InlineKeyboardButton button2 = new InlineKeyboardButton("Убрать").callbackData(callBackData(chatId, callbackData, false));
        markup.addRow(button, button2);
        SendPhoto sendPhoto = new SendPhoto(chatId, new File(imagePath)).caption(caption).replyMarkup(markup);
        bot.execute(sendPhoto);
    }

    private static String callBackData(long chatId, String callbackData, boolean add) {
        CallbackDataBean callbackDataBean = new CallbackDataBean();
        callbackDataBean.setChatId(chatId);
        callbackDataBean.setAdd(add);
        callbackDataBean.setOrder(callbackData);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(callbackDataBean);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static CallbackDataBean readCallBackData(String str) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(str, CallbackDataBean.class);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void sendOrderButton(TelegramBot bot, long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton button = new InlineKeyboardButton("Оформить заказ").callbackData(callBackData(chatId, ORDER, true));
        markup.addRow(button);
        SendMessage message = new SendMessage(chatId, "Показать /menu");
        message.replyMarkup(markup);
        bot.execute(message);
    }

    private static void createOrderTotal(TelegramBot bot, long chatId, String callbackId) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackId);
        bot.execute(answer);
        ConcurrentHashMap<String, AtomicInteger> currentOrders = orders.get(chatId);
        if (!currentOrders.isEmpty()) {
            StringBuilder sb = new StringBuilder("Ваш заказ:\n");
            currentOrders.forEach((k, v) -> sb.append(k).append(": ").append(v).append("\n"));
            SendMessage outMess = new SendMessage(chatId, sb.toString());
            bot.execute(outMess);
        } else {
            bot.execute(new SendMessage(chatId, "В заказе ничего нет"));
        }
    }

    private static void createMessageSomethingWasAdded(TelegramBot bot, long chatId, String order, String callbackId) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackId);
        bot.execute(answer);
        orders.putIfAbsent(chatId, new ConcurrentHashMap<>());
        orders.get(chatId).putIfAbsent(order, new AtomicInteger(0));
        orders.get(chatId).get(order).incrementAndGet();
        SendMessage outMess = new SendMessage(chatId, "Добавлена одна " + order + " в заказ.");
        bot.execute(outMess);
    }

    private static void createMessageSomethingWasRemoved(TelegramBot bot, long chatId, String order, String callbackId) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackId);
        bot.execute(answer);
        orders.putIfAbsent(chatId, new ConcurrentHashMap<>());
        orders.get(chatId).putIfAbsent(order, new AtomicInteger(0));
        orders.get(chatId).get(order).accumulateAndGet(-1, (i1, i2) -> {
            return i1 + i2 < 0 ? 0 : i1 + i2;
        });
        orders.get(chatId).computeIfPresent(order, (k, v) -> v.get() == 0 ? null : v);
        SendMessage outMess = new SendMessage(chatId, "Убрана одна " + order + " из заказа.");
        bot.execute(outMess);
    }

    private static void createMessageWithKeyboard(TelegramBot bot, long chatId) {
        SendMessage outMess = new SendMessage(chatId, " ");
        outMess.setReplyMarkup(initKeyboard());
        bot.execute(outMess);
    }

    private static ReplyKeyboardMarkup initKeyboard() {
        //Создаем объект будущей клавиатуры и выставляем нужные настройки
        KeyboardButton keyboardButton = new KeyboardButton("Оформить заказ");
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup(keyboardButton);
        replyKeyboardMarkup.resizeKeyboard(true);
        return replyKeyboardMarkup;
    }

}
