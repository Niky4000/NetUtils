package ru.kiokle.telegrambot;

import ru.kiokle.telegrambot.bean.CallbackDataBean;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import ru.kiokle.telegrambot.bean.CallbackOrderRecievedBean;
import ru.kiokle.telegrambot.bean.OrderBean;
import ru.kiokle.telegrambot.bean.OrderConfigsBean;
import ru.kiokle.telegrambot.database.MasterUserKey;
import ru.kiokle.telegrambot.database.NotificationBean;
import ru.kiokle.telegrambot.db.bean.Order;
import ru.kiokle.telegrambot.db.bean.OrderKey;
import ru.kiokle.telegrambot.bean.PaymentBean;
import ru.kiokle.telegrambot.db.bean.Payment;
import ru.kiokle.telegrambot.db.bean.UserOrder;
import ru.kiokle.telegrambot.enums.Messages;
import static ru.kiokle.telegrambot.enums.Messages.ADD;
import static ru.kiokle.telegrambot.enums.Messages.CANCEL_THIS_ORDER;
import static ru.kiokle.telegrambot.enums.Messages.ORDER;
import static ru.kiokle.telegrambot.enums.Messages.ORDER_HAS_BEEN_GIVEN;
import static ru.kiokle.telegrambot.enums.Messages.SHOW_MY_ORDERS;
import ru.kiokle.telegrambot.utils.MessageUtils;
import static ru.kiokle.telegrambot.utils.PriceUtils.getSum;

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
    private final String ADD_ME_TO_MASTER_USERS = "добавь меня в получатели";
    private final String REMOVE_ME_FROM_MASTER_USERS = "убери меня из получателей";
    private static final String THE_ORDER_WAS_GIVEN = "заказ передан ";
    private static final String THE_ORDER_WAS_NOT_GIVEN = "заказ не передан ";

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
                    } else if (checkMasterUser(update) && (update.message() != null || (update.callbackQuery() != null && update.callbackQuery().data() != null))) {
                        if (update.callbackQuery() != null && update.callbackQuery().data() != null) {
                            CallbackOrderRecievedBean callBackData = CallbackOrderRecievedBean.fromString(update.callbackQuery().data());
                            if (callBackData.getMessage().equals(ORDER_HAS_BEEN_GIVEN)) {
                                try {
                                    Long userChatId = callBackData.getUserChatId();
                                    Long masterChatId = callBackData.getChatId();
                                    UserOrder userOrder = h2.getUserOrder(callBackData.getOrderId());
                                    h2.updateUserOrder(userOrder.setFinished(true));
                                    String callbackId = update.callbackQuery().id();
                                    createMessageAnswer(bot, masterChatId, callbackId, "Заказ " + userOrder.getId() + " завершён!");
                                    createMessageAnswer(bot, userChatId, null, "Заказ " + userOrder.getId() + " передан!");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } else if (update.message() != null && MENU.equals(update.message().text())) {
                            showMainMenu(update, bot);
                        } else if (update.message() != null && ORDERS.equals(update.message().text())) {
                            showOrders(update, bot);
                        } else if (update.message() != null && PAID.equals(update.message().text())) {
                            showPaidOrders(update, bot);
                        } else if (update.message() != null && WHO_ORDERED.equals(update.message().text())) {
                            showOrdersByUsers(update, bot);
                        } else if (update.message() != null && update.message().text() != null && (update.message().text().toLowerCase().startsWith(THE_ORDER_WAS_NOT_GIVEN) || update.message().text().toLowerCase().startsWith(THE_ORDER_WAS_GIVEN))) {
                            long chatId = update.message().chat().id();
                            String str = update.message().text().toLowerCase().replace(THE_ORDER_WAS_NOT_GIVEN, "").replace(THE_ORDER_WAS_GIVEN, "");
                            try {
                                Long orderId = Long.valueOf(str);
                                UserOrder userOrder = h2.getUserOrder(orderId);
                                ru.kiokle.telegrambot.db.bean.User user = h2.getUserById(userOrder.getUserId());
                                h2.updateUserOrder(userOrder.setFinished(update.message().text().toLowerCase().startsWith(THE_ORDER_WAS_NOT_GIVEN) ? false : true));
                                createMessageAnswer(bot, chatId, null, "Заказ " + orderId + " помечен, как не переданный!");
                                createMessageAnswer(bot, user.getChatId(), null, "Заказ " + userOrder.getId() + " помечен, как не переданный!");
                            } catch (Exception e) {
                                createMessageAnswer(bot, chatId, null, "Не понятен номер заказа!");
                            }
                        } else {
                            showMasterUserMenu(update, bot);
                        }
                    } else if (update.message() != null) {
                        showMainMenu(update, bot);
                    } else if (update.callbackQuery() != null && update.callbackQuery().data() != null) {
                        CallbackDataBean callBackData = CallbackDataBean.fromString(update.callbackQuery().data());
                        Long chatId = callBackData.getChatId();
                        String callbackId = update.callbackQuery().id();
                        if (ORDER.equals(callBackData.getMessage())) {
                            ru.kiokle.telegrambot.db.bean.User user = h2.getUserByChatId(chatId);
                            if (user != null) {
                                ConcurrentHashMap<OrderKey, AtomicInteger> currentOrders = orders.get(chatId);
                                if (currentOrders != null && currentOrders.entrySet() != null) {
                                    List<Order> orderList = currentOrders.entrySet().stream().map(e -> new Order(user.getId(), e.getKey().getName(), getSum(e.getKey().getName(), e.getValue().get(), configs.getPriceMap()), e.getValue().get())).collect(Collectors.toList());
                                    UserOrder order = h2.order(user.getId(), orderList);
                                    createOrderTotal(bot, chatId, callbackId, order, user, orderList);
                                } else {
                                    List<Order> orderList = restoreOrderMap(chatId);
                                    UserOrder order = h2.getActiveUserOrder(user.getId());
                                    createOrderTotal(bot, chatId, callbackId, order, user, orderList);
                                }
                            }
                        } else if (SHOW_MY_ORDERS.equals(callBackData.getMessage())) {
                            List<Order> activeOrdersOfTheUser = h2.getActiveOrdersOfTheUser(chatId);
                            if (!activeOrdersOfTheUser.isEmpty()) {
                                ru.kiokle.telegrambot.db.bean.User user = h2.getUserByChatId(chatId);
                                UserOrder userOrder = h2.getActiveUserOrder(user.getId());
                                String orderMessage = getOrderMessage(activeOrdersOfTheUser);
                                showOrderInfoButton(bot, chatId, userOrder.getId(), callbackId, orderMessage);
                            } else {
                                createMessageAnswer(bot, chatId, callbackId, "Нет активных заказов!");
                            }
                        } else if (CANCEL_THIS_ORDER.equals(callBackData.getMessage())) {
                            List<Order> activeOrdersOfTheUser = h2.getActiveOrdersOfTheUser(chatId);
                            if (!activeOrdersOfTheUser.isEmpty()) {
                                ru.kiokle.telegrambot.db.bean.User user = h2.getUserByChatId(chatId);
                                UserOrder userOrder = h2.getActiveUserOrder(user.getId());
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
                        } else if (callBackData.getMessage().equals(ADD) && callBackData.isAdd()) {
                            createMessageSomethingWasAdded(bot, chatId, callBackData.getOrder(), callbackId);
                        } else if (callBackData.getMessage().equals(ADD) && !callBackData.isAdd()) {
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

    private static final String MENU = "/menu";
    private static final String ORDERS = "/orders";
    private static final String PAID = "/paid";
    private static final String WHO_ORDERED = "/whoOrdered";

    private boolean checkMasterUser(Update update) {
        if (checkForSystemCommands(update, str -> MessageUtils.fromString(str))) {
            return true;
        } else if (update.message() != null && update.message().from() != null) {
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

    private final Set<Messages> systemCommands = Set.of(ORDER_HAS_BEEN_GIVEN);

    private boolean checkForSystemCommands(Update update, Function<String, Messages> getMessage) {
        if (update.callbackQuery() != null && update.callbackQuery().data() != null) {
            Messages message = getMessage.apply(update.callbackQuery().data());
            return systemCommands.contains(message);
        } else {
            return false;
        }
    }

    private void showMasterUserMenu(Update update, TelegramBot bot) {
        long chatId = update.message().chat().id();
        createMessageAnswer(bot, chatId, null,
                MENU + " - Отобразить меню\n"
                + ORDERS + " - Что заказано?\n"
                + PAID + " - Что оплачено?\n"
                + WHO_ORDERED + " - Кто и что заказал?\n");
    }

    private void showOrders(Update update, TelegramBot bot) {
        long chatId = update.message().chat().id();
        List<Order> orders = h2.getActiveOrders();
        if (!orders.isEmpty()) {
            Map<String, Integer> map = new TreeMap<>(orders.stream().collect(Collectors.toMap(Order::getName, Order::getQuantity, (q1, q2) -> q1 + q2)));
            StringBuilder sb = new StringBuilder("-- Заказы --\n");
            for (Entry<String, Integer> order : map.entrySet()) {
                sb.append(order.getKey()).append(" - ").append(order.getValue()).append(" шт.\n");
            }
            sb.append("Сумма: " + orders.stream().mapToLong(o -> o.getPrice()).sum() + " руб.\n");
            sb.append("------------");
            createMessageAnswer(bot, chatId, null, sb.toString());
        } else {
            createMessageAnswer(bot, chatId, null, "Нет заказов.");
        }
    }

    private void showPaidOrders(Update update, TelegramBot bot) {
        long chatId = update.message().chat().id();
        List<UserOrder> paidUserOrderList = h2.getPaidUserOrder();
        if (!paidUserOrderList.isEmpty()) {
            createMessageAnswer(bot, chatId, null, "-- Заказы --");
            for (UserOrder userOrder : paidUserOrderList) {
                StringBuilder sb = new StringBuilder();
                ru.kiokle.telegrambot.db.bean.User user = h2.getUserById(userOrder.getUserId());
                List<Order> orders = h2.getOrdersByUserOrderId(userOrder.getId());
                Map<String, Integer> map = new TreeMap<>(orders.stream().collect(Collectors.toMap(Order::getName, Order::getQuantity, (q1, q2) -> q1 + q2)));
                sb.append(user.getLogin()).append(" заказ № ").append(userOrder.getId()).append(":\n");
                for (Entry<String, Integer> order : map.entrySet()) {
                    sb.append(order.getKey()).append(" - ").append(order.getValue()).append(" шт.\n");
                }
                sb.append("Сумма: " + orders.stream().mapToLong(o -> o.getPrice()).sum() + " руб.\n");
//                createMessageAnswer(bot, chatId, null, sb.toString());
                showButtonWithText(bot, chatId, "Заказ передан", sb.toString(), new CallbackOrderRecievedBean(ORDER_HAS_BEEN_GIVEN, chatId, user.getChatId(), userOrder.getId()).toString());
            }
//            createMessageAnswer(bot, chatId, null, "------------");
        } else {
            createMessageAnswer(bot, chatId, null, "Нет оплаченных заказов.");
        }
    }

    private void showOrdersByUsers(Update update, TelegramBot bot) {
        long chatId = update.message().chat().id();
        List<Order> orders = h2.getActiveOrders();
        if (!orders.isEmpty()) {
            Map<Long, Map<String, Integer>> userIdMap = new TreeMap<>(orders.stream().collect(Collectors.groupingBy(Order::getUserId, Collectors.collectingAndThen(Collectors.toList(), list -> new TreeMap<>(list.stream().collect(Collectors.toMap(Order::getName, Order::getQuantity, (q1, q2) -> q1 + q2)))))));
            createMessageAnswer(bot, chatId, null, "-- Заказы --");
            for (Entry<Long, Map<String, Integer>> entry : userIdMap.entrySet()) {
                Long userId = entry.getKey();
                StringBuilder sb = new StringBuilder();
                Map<String, Integer> map = entry.getValue();
                ru.kiokle.telegrambot.db.bean.User user = h2.getUserById(userId);
                UserOrder userOrder = h2.getActiveUserOrder(user.getId());
                sb.append(user.getLogin()).append(" заказ № ").append(userOrder.getId()).append(":\n");
                for (Entry<String, Integer> order : map.entrySet()) {
                    sb.append(order.getKey()).append(" - ").append(order.getValue()).append(" шт.\n");
                }
                if (userOrder.isPaid()) {
                    sb.append("Оплачено!\n");
                } else {
                    sb.append("Не оплачено!\n");
                }
                sb.append("Сумма: " + orders.stream().mapToLong(o -> o.getPrice()).sum() + " руб.\n");
                showButtonWithText(bot, chatId, "Заказ передан", sb.toString(), new CallbackOrderRecievedBean(ORDER_HAS_BEEN_GIVEN, chatId, user.getChatId(), userOrder.getId()).toString());
            }
        } else {
            createMessageAnswer(bot, chatId, null, "Нет заказов.");
        }
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
        InlineKeyboardButton button = new InlineKeyboardButton("Заказать").callbackData(new CallbackDataBean(ADD, callbackData, true, chatId).toString());
        InlineKeyboardButton button2 = new InlineKeyboardButton("Убрать").callbackData(new CallbackDataBean(ADD, callbackData, false, chatId).toString());
        markup.addRow(button, button2);
        SendPhoto sendPhoto = new SendPhoto(chatId, new File(imagePath)).caption(caption).replyMarkup(markup);
        bot.execute(sendPhoto);
    }

    private void sendOrderButton(TelegramBot bot, long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton button = new InlineKeyboardButton("Оформить заказ").callbackData(new CallbackDataBean(ORDER, "", true, chatId).toString());
        markup.addRow(button);
        SendMessage message = new SendMessage(chatId, "Показать /menu");
        message.replyMarkup(markup);
        bot.execute(message);
    }

    private void showMyOrdersButton(TelegramBot bot, long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton button = new InlineKeyboardButton("Показать мои заказы").callbackData(new CallbackDataBean(SHOW_MY_ORDERS, "", true, chatId).toString());
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
        InlineKeyboardButton button = new InlineKeyboardButton("Отменить заказ").callbackData(new CallbackDataBean(CANCEL_THIS_ORDER, "", true, chatId).toString());
        markup.addRow(button);
        SendMessage message = new SendMessage(chatId, "Заказ № " + orderId + ":\n" + orderMessage);
        message.replyMarkup(markup);
        bot.execute(message);
    }

    private void showButtonWithText(TelegramBot bot, long chatId, String buttonText, String text, String callBackData) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton button = new InlineKeyboardButton(buttonText).callbackData(callBackData);
        markup.addRow(button);
        SendMessage message = new SendMessage(chatId, text);
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
                notifications.add(new NotificationBean(chatId, "Пользователь " + user.getLogin() + " сделал заказ № " + userOrder.getId() + ":\n" + orderMessage + "На сумму " + userOrder.getPrice() + "!"));
            }
            notificationThread.interrupt();
            StringBuilder message = new StringBuilder("Ваш заказ № " + userOrder.getId() + ":\n").append(orderMessage);
            if (paymentEnabled) {
                List<Payment> activePayments = h2.getActivePayments(userOrder.getId());
                PaymentBean payment;
                if (!(!activePayments.isEmpty() && activePayments.get(activePayments.size() - 1).getPrice() == userOrder.getPrice())) {
                    payment = yookassa.createPayment(createIdempotenceKey(), userOrder);
                } else {
                    payment = h2.getFirstPendingPaymentResponse(activePayments.get(activePayments.size() - 1).getId());
                }
                message.append("\n").append("Ваша ссылка на оплату ").append(userOrder.getPrice()).append(" руб").append(":\n").append(payment.getConfirmationUrl());
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
