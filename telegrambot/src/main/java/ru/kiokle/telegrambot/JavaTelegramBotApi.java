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
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import ru.kiokle.telegrambot.bean.OrderBean;
import ru.kiokle.telegrambot.bean.OrderConfigsBean;
import ru.kiokle.telegrambot.database.MasterUserKey;
import ru.kiokle.telegrambot.database.NotificationBean;
import ru.kiokle.telegrambot.db.bean.Order;
import ru.kiokle.telegrambot.db.bean.OrderKey;
import ru.kiokle.telegrambot.bean.PaymentBean;
import ru.kiokle.telegrambot.db.bean.Payment;
import ru.kiokle.telegrambot.db.bean.UserOrder;
import ru.kiokle.telegrambot.utils.PriceUtils;

/**
 *
 * @author me
 */
public class JavaTelegramBotApi {

    OrderConfigsBean configs;
    H2 h2;
    AtomicReference<TelegramBot> botReference = new AtomicReference<>();
    Thread notificationThread;
    Yookassa yookassa;
    boolean paymentEnabled;
    private final String ORDER = "order";
    private final String SHOW_MY_ORDERS = "show_my_orders";
    private final String CANCEL_THIS_ORDER = "cancel_this_order";
    private final String ADD_ME_TO_MASTER_USERS = "добавь меня в получатели";
    private final String REMOVE_ME_FROM_MASTER_USERS = "убери меня из получателей";

    public JavaTelegramBotApi() throws IOException {
        FileUtils fileUtils = new FileUtils();
        configs = fileUtils.getConfigs();
        h2 = new H2(fileUtils, configs);
        createOrderCleanThread();
        List<MasterUserKey> allMasterUsers = h2.getAllMasterUsers();
        masterUsersSet.addAll(allMasterUsers);
        createNotificationThread();
        yookassa = new Yookassa(fileUtils, h2);
        h2.setYookassa(yookassa);
        paymentEnabled = Boolean.valueOf((String) fileUtils.getSystemProperties().get("payment_enabled"));
        if (Boolean.valueOf((String) fileUtils.getSystemProperties().get("yookassaActiveProbing"))) {
            createYookassaActiveProbingThread();
        }
    }

    private void createYookassaActiveProbingThread() {
        Thread yookassaThread = new Thread(() -> {
            while (true) {
                h2.checkActivePayments((Map.Entry<ru.kiokle.telegrambot.db.bean.User, UserOrder> userEntry, PaymentBean payment) -> {
                    synchronized (notifications) {
                        notifications.add(new NotificationBean(userEntry.getKey().getChatId(), "Пользователь " + userEntry.getKey().getLogin() + " успешно оплатил заказ № " + userEntry.getValue().getId() + " на сумму " + payment.getAmount() + " руб! Будет зачислено " + payment.getIncomeAmount() + " руб!"));
                    }
                    notificationThread.interrupt();
                });
                waitSomeTime(60);
            }
        }, "yookassaThread");
        yookassaThread.setDaemon(true);
        yookassaThread.start();
    }

    private void createNotificationThread() {
        notificationThread = new Thread(() -> {
            while (true) {
                boolean interrupted = Thread.interrupted();
                if (botReference.get() != null) {
                    synchronized (notifications) {
                        if (!notifications.isEmpty()) {
                            TelegramBot bot = botReference.get();
                            Iterator<NotificationBean> iterator = notifications.iterator();
                            while (iterator.hasNext()) {
                                NotificationBean bean = iterator.next();
                                for (MasterUserKey master : masterUsersSet) {
                                    createMessageAnswer(bot, master.getChatId(), null, bean.getMessage());
                                }
                            }
                            notifications.clear();
                        }
                    }
                }
                waitSomeTime(10);
            }
        }, "notificationThread");
        notificationThread.setDaemon(true);
        notificationThread.start();
    }

    private void createOrderCleanThread() {
        Thread ordersClean = new Thread(() -> {
            while (true) {
                cleanOrders();
//                cleanMasterUsers();
                waitSomeTime(10);
            }
        }, "ordersClean");
        ordersClean.setDaemon(true);
        ordersClean.start();
    }

    private final ConcurrentHashMap<Long, ConcurrentHashMap<OrderKey, AtomicInteger>> orders = new ConcurrentHashMap<>();
    private final CopyOnWriteArraySet<MasterUserKey> masterUsersSet = new CopyOnWriteArraySet<>();
    private final List<NotificationBean> notifications = new ArrayList();

    public void start(String[] args) {
        if (args.length > 0) {
            String token = args[0];
// Create your bot passing the token received from @BotFather
            TelegramBot bot = new TelegramBot(token);

// Register for updates
            bot.setUpdatesListener(updates -> {
                // ... process updates
                // return id of last processed update or confirm them all
                updates.forEach(update -> {
                    if (update.message() != null && update.message().text() != null && ADD_ME_TO_MASTER_USERS.equals(update.message().text().toLowerCase())) {
                        long chatId = update.message().chat().id();
                        User user = update.message().from();
                        h2.masterUser(user.username(), true, chatId);
                        masterUsersSet.add(new MasterUserKey(update.message().from().username(), chatId));
                        createMessageAnswer(bot, chatId, null, "Пользователь " + user.username() + " был добавлен в качестве получателя сообщений о заказах!");
                    } else if (update.message() != null && update.message().text() != null && REMOVE_ME_FROM_MASTER_USERS.equals(update.message().text().toLowerCase())) {
                        long chatId = update.message().chat().id();
                        User user = update.message().from();
                        h2.masterUser(user.username(), false, chatId);
                        masterUsersSet.remove(new MasterUserKey(update.message().from().username()));
                        createMessageAnswer(bot, chatId, null, "Пользователь " + user.username() + " был удалён из получателей сообщений о заказах!");
                    } else if (update.message() != null) {
                        if (checkMasterUser(update)) {
                            showMainMenu(update, bot);
                            showMasterUserMenu(update, bot);
                        } else {
                            showMainMenu(update, bot);
                        }
                    } else if (update.callbackQuery() != null && update.callbackQuery().data() != null) {
                        CallbackDataBean callBackData = readCallBackData(update.callbackQuery().data());
                        Long chatId = callBackData.getChatId();
                        String callbackId = update.callbackQuery().id();
                        if (ORDER.equals(callBackData.getOrder())) {
                            ru.kiokle.telegrambot.db.bean.User user = h2.getUserByChatId(chatId);
                            if (user != null) {
                                ConcurrentHashMap<OrderKey, AtomicInteger> currentOrders = orders.get(chatId);
                                if (currentOrders != null && currentOrders.entrySet() != null) {
                                    List<Order> orderList = currentOrders.entrySet().stream().map(e -> new Order(user.getId(), e.getKey().getName(), 0, e.getValue().get())).collect(Collectors.toList());
                                    UserOrder order = h2.order(user.getId(), orderList);
                                    createOrderTotal(bot, chatId, callbackId, order.setPrice(PriceUtils.getSum(orderList, configs.getPriceMap())), user, orderList);
                                } else {
                                    UserOrder order = h2.getActiveUserOrder(user.getId());
                                    List<Order> orderList = restoreOrderMap(chatId);
                                    createOrderTotal(bot, chatId, callbackId, order.setPrice(PriceUtils.getSum(orderList, configs.getPriceMap())), user, orderList);
                                }
                            }
                        } else if (SHOW_MY_ORDERS.equals(callBackData.getOrder())) {
                            List<Order> activeOrdersOfTheUser = h2.getActiveOrdersOfTheUser(chatId);
                            if (!activeOrdersOfTheUser.isEmpty()) {
                                ru.kiokle.telegrambot.db.bean.User user = h2.getUserByChatId(chatId);
                                UserOrder userOrder = h2.getActiveUserOrder(user.getId());
                                String orderMessage = getOrderMessage(activeOrdersOfTheUser);
                                showOrderInfoButton(bot, chatId, userOrder.getId(), callbackId, orderMessage);
                            } else {
                                createMessageAnswer(bot, chatId, callbackId, "Нет активных заказов!");
                            }
                        } else if (CANCEL_THIS_ORDER.equals(callBackData.getOrder())) {
                            List<Order> activeOrdersOfTheUser = h2.getActiveOrdersOfTheUser(chatId);
                            if (!activeOrdersOfTheUser.isEmpty()) {
                                ru.kiokle.telegrambot.db.bean.User user = h2.getUserByChatId(chatId);
                                UserOrder userOrder = h2.getActiveUserOrder(user.getId());
                                List<Order> orderList = restoreOrderMap(chatId);
                                userOrder.setPrice(PriceUtils.getSum(orderList, configs.getPriceMap()));
                                h2.cancelUserOrder(userOrder);
                                orders.computeIfPresent(chatId, (k, v) -> null);
                                synchronized (notifications) {
                                    notifications.add(new NotificationBean(chatId, "Пользователь " + user.getLogin() + " отменил заказ № " + userOrder.getId() + " на сумму " + userOrder.getPrice() + " руб!"));
                                }
                                notificationThread.interrupt();
                                createMessageAnswer(bot, chatId, callbackId, "Заказ № " + userOrder.getId() + " был отменён!");
                            } else {
                                orders.computeIfPresent(chatId, (k, v) -> null);
                                createMessageAnswer(bot, chatId, callbackId, "Нет активных заказов!");
                            }
                        } else if (callBackData.isAdd()) {
                            createMessageSomethingWasAdded(bot, chatId, callBackData.getOrder(), callbackId);
                        } else if (!callBackData.isAdd()) {
                            createMessageSomethingWasRemoved(bot, chatId, callBackData.getOrder(), callbackId);
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
            botReference.set(bot);
        }
    }

    private boolean checkMasterUser(Update update) {
        if (update.message() != null && update.message().from() != null) {
            if (masterUsersSet.contains(new MasterUserKey(update.message().from().username()))) {
                return true;
            } else {
                MasterUserKey masterUser = h2.selectMasterUser(update.message().from().username());
                if (masterUser.getId() > 0) {
                    masterUsersSet.add(masterUser);
                }
                return masterUser.getId() > 0;
            }
        } else {
            return false;
        }
    }

    private void showMasterUserMenu(Update update, TelegramBot bot) {

    }

    private void showMainMenu(Update update, TelegramBot bot) {
        long chatId = update.message().chat().id();
        User user = update.message().from();
        String username = user.username();
        ru.kiokle.telegrambot.db.bean.User userDb = h2.getUserByChatId(chatId);
        if (userDb == null) {
            h2.addUser(username, null, null, chatId);
        }
        for (OrderBean order : configs.getOrders()) {
            sendPhotoWithButton(bot, chatId, order.getPathToPicture(), order.getDescription() + ", " + order.getPrice() + " " + order.getCurrency(), order.getName());
        }
        sendOrderButton(bot, chatId);
        List<Order> activeOrdersOfTheUser = h2.getActiveOrdersOfTheUser(chatId);
        if (!activeOrdersOfTheUser.isEmpty()) {
            showMyOrdersButton(bot, chatId);
        }
//                    SendResponse response = bot.execute(new SendMessage(chatId, "Hello!"));
//                        sendPhotoWithButton(bot, chatId);
//                        sendFotos(bot, chatId);
//                        sendFotos2(bot, chatId);
    }

    private List<Order> restoreOrderMap(Long chatId) {
        List<Order> orderList = h2.getActiveOrdersOfTheUser(chatId);
        ConcurrentHashMap<OrderKey, AtomicInteger> concurrentHashMap = new ConcurrentHashMap<>();
        orderList.forEach(o -> concurrentHashMap.put(new OrderKey(o.getName()), new AtomicInteger(o.getQuantity())));
        orders.putIfAbsent(chatId, concurrentHashMap);
        return orderList;
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

    private void sendPhotoWithButton(TelegramBot bot, long chatId, String imagePath, String caption, String callbackData) {
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
//        callbackDataBean.setUsername(username);
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

    private void sendOrderButton(TelegramBot bot, long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton button = new InlineKeyboardButton("Оформить заказ").callbackData(callBackData(chatId, ORDER, true));
        markup.addRow(button);
        SendMessage message = new SendMessage(chatId, "Показать /menu");
        message.replyMarkup(markup);
        bot.execute(message);
    }

    private void showMyOrdersButton(TelegramBot bot, long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton button = new InlineKeyboardButton("Показать мои заказы").callbackData(callBackData(chatId, SHOW_MY_ORDERS, true));
        markup.addRow(button);
        SendMessage message = new SendMessage(chatId, "Показать /menu");
        message.replyMarkup(markup);
        bot.execute(message);
    }

    private void showOrderInfoButton(TelegramBot bot, long chatId, long orderId, String callbackId, String orderMessage) {
        if (callbackId != null) {
            AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackId);
            bot.execute(answer);
        }
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton button = new InlineKeyboardButton("Отменить заказ").callbackData(callBackData(chatId, CANCEL_THIS_ORDER, true));
        markup.addRow(button);
        SendMessage message = new SendMessage(chatId, "Заказ № " + orderId + ":\n" + orderMessage);
        message.replyMarkup(markup);
        bot.execute(message);
    }

    private void createOrderTotal(TelegramBot bot, long chatId, String callbackId, UserOrder userOrder, ru.kiokle.telegrambot.db.bean.User user, List<Order> orderList) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackId);
        bot.execute(answer);
//        ConcurrentHashMap<String, AtomicInteger> currentOrders = orders.get(chatId);
        if (!orderList.isEmpty()) {
            String orderMessage = getOrderMessage(orderList);
            synchronized (notifications) {
                notifications.add(new NotificationBean(chatId, "Пользователь " + user.getLogin() + " сделал заказ № " + userOrder.getId() + ":\n" + orderMessage));
            }
            notificationThread.interrupt();
            StringBuilder message = new StringBuilder("Ваш заказ № " + userOrder.getId() + ":\n").append(orderMessage);
            if (paymentEnabled) {
                List<Payment> activePayments = h2.getActivePayments(userOrder.getId());
                if (!(activePayments.size() == 1 && activePayments.get(0).getPrice() == userOrder.getPrice())) {
//                activePayments.forEach(payment -> yookassa.cancelPayment(payment.getPaymentId(), payment.getIdempotenceKey()));
                    PaymentBean payment = yookassa.createPayment(createIdempotenceKey(), userOrder);
                    message.append("\n").append("Ваша ссылка на оплату ").append(userOrder.getPrice()).append(" руб").append(":\n").append(payment.getConfirmationUrl());
                }
            }
            SendMessage outMess = new SendMessage(chatId, message.toString());
            bot.execute(outMess);
        } else {
            bot.execute(new SendMessage(chatId, "В заказе ничего нет"));
        }
    }

    private static String createIdempotenceKey() {
        return UUID.randomUUID().toString().replace("-", "").toLowerCase();
    }

    private String getOrderMessage(List<Order> orderList) {
        StringBuilder sb = new StringBuilder();
        orderList.forEach(o -> sb.append(o.getName()).append(": ").append(o.getQuantity()).append(" шт.").append("\n"));
        return sb.toString();
    }

    private void createMessageAnswer(TelegramBot bot, long chatId, String callbackId, String message) {
        if (callbackId != null) {
            AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackId);
            bot.execute(answer);
        }
        SendMessage outMess = new SendMessage(chatId, message);
        bot.execute(outMess);
    }

    private void createMessageSomethingWasAdded(TelegramBot bot, long chatId, String order, String callbackId) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackId);
        bot.execute(answer);
        orders.putIfAbsent(chatId, new ConcurrentHashMap<>());
        orders.get(chatId).putIfAbsent(new OrderKey(order), new AtomicInteger(0));
        orders.get(chatId).get(new OrderKey(order)).incrementAndGet();
        SendMessage outMess = new SendMessage(chatId, "Добавлена одна " + order + " в заказ.");
        bot.execute(outMess);
    }

    private void createMessageSomethingWasRemoved(TelegramBot bot, long chatId, String order, String callbackId) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackId);
        bot.execute(answer);
        orders.putIfAbsent(chatId, new ConcurrentHashMap<>());
        orders.get(chatId).putIfAbsent(new OrderKey(order), new AtomicInteger(0));
        orders.get(chatId).get(new OrderKey(order)).accumulateAndGet(-1, (i1, i2) -> {
            return i1 + i2 < 0 ? 0 : i1 + i2;
        });
        orders.get(chatId).computeIfPresent(new OrderKey(order), (k, v) -> v.get() == 0 ? null : v);
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

    private void waitSomeTime(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ex) {
//            ex.printStackTrace();
        }
    }

    private void cleanMasterUsers() {
        masterUsersSet.removeIf(u -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_MONTH, -1);
//            calendar.add(Calendar.MINUTE, -1);
            Date yesterday = calendar.getTime();
            return u.getCreated().before(yesterday);
        });
    }

    private void cleanOrders() {
        ConcurrentHashMap.KeySetView<Long, ConcurrentHashMap<OrderKey, AtomicInteger>> keySet = orders.keySet();
        keySet.forEach(key -> {
            orders.computeIfPresent(key, (chatId, map) -> {
                if (map != null && !map.isEmpty()) {
                    ConcurrentHashMap.KeySetView<OrderKey, AtomicInteger> keys = map.keySet();
                    keys.forEach(k -> {
                        map.computeIfPresent(k, (OrderKey ok, AtomicInteger v) -> {
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTime(new Date());
                            calendar.add(Calendar.DAY_OF_MONTH, -1);
//                                    calendar.add(Calendar.MINUTE, -1);
                            Date yesterday = calendar.getTime();
                            if (ok.getCreated().before(yesterday)) {
                                return null;
                            } else {
                                return v;
                            }
                        });
                    });
                    return map.isEmpty() ? null : map;
                } else {
                    return null;
                }
            });
        });
    }
}
