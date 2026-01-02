package ru.kiokle.telegrambot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import ru.kiokle.telegrambot.bean.OrderConfigsBean;
import ru.kiokle.telegrambot.database.DatabaseUpdates;
import ru.kiokle.telegrambot.database.MasterUserKey;
import ru.kiokle.telegrambot.db.bean.Order;
import ru.kiokle.telegrambot.bean.PaymentBean;
import ru.kiokle.telegrambot.db.bean.Payment;
import ru.kiokle.telegrambot.db.bean.User;
import ru.kiokle.telegrambot.db.bean.UserOrder;
import ru.kiokle.telegrambot.utils.PriceUtils;

public class H2 {

    private final OrderConfigsBean configs;
    private Yookassa yookassa;
    private String jdbcUrl;
    private String username = "sa"; // default H2 username
    private String password = ""; // default H2 password
    private BlockingQueue<Connection> connectionQueue = new LinkedBlockingQueue<>();
    private static final int TIME_TO_WAIT = 10;
    private static final int POLL_TIMEOUT = 2;
    private static final String MASTER_USER = "master_user";
    private static final String USERS = "users";
    private static final String USER_ORDERS = "user_orders";
    private static final String ORDERS = "orders";
    private static final String PAYMENTS = "payments";
    private static final String ORDERS_COLUMNS = "o.id,o.user_id,o.name,o.price,o.quantity,uo.created,uo.finished,o.active";
    private AtomicLong masterUserId;
    private AtomicLong userId;
    private AtomicLong userOrderId;
    private AtomicLong orderId;
    private AtomicLong paymentId;

    public H2(FileUtils fileUtils, OrderConfigsBean configs) {
        this.configs = configs;
        jdbcUrl = "jdbc:h2:file:" + fileUtils.getDatabaseFile();
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            new DatabaseUpdates().update(connection);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        masterUserId = getMaxId(MASTER_USER);
        userId = getMaxId(USERS);
        userOrderId = getMaxId(USER_ORDERS);
        orderId = getMaxId(ORDERS);
        paymentId = getMaxId(PAYMENTS);
        Thread h2 = new Thread(() -> {
            if (connectionQueue.size() > 1) {
                try {
                    Connection conn = connectionQueue.poll(POLL_TIMEOUT, TimeUnit.SECONDS);
                    conn.close();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            try {
                Thread.sleep(TIME_TO_WAIT * 1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        });
        h2.setName("H2");
        h2.start();
    }

    public void setYookassa(Yookassa yookassa) {
        this.yookassa = yookassa;
    }

    private void addConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
        connectionQueue.add(conn);
    }

    private void handle(Consumer<Connection> connectionConsumer) {
        try {
            Connection connection = connectionQueue.poll();
            if (connection == null) {
                connection = DriverManager.getConnection(jdbcUrl, username, password);
            }
            connectionConsumer.accept(connection);
            connectionQueue.add(connection);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private <T> T handle(Function<Connection, T> connectionConsumer) {
        try {
            Connection connection = connectionQueue.poll();
            if (connection == null) {
                connection = DriverManager.getConnection(jdbcUrl, username, password);
            }
            T apply = connectionConsumer.apply(connection);
            connectionQueue.add(connection);
            return apply;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private AtomicLong getMaxId(String table) {
        AtomicLong id = new AtomicLong(0L);
        handle(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("select max(id) from " + table)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        id.set(resultSet.getLong(1));
                        id.incrementAndGet();
                    }
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
        return id;
    }

    public void masterUser(String user, boolean active, long chatId) {
        long id = selectMasterUser(user).getId();
        if (id == -1L) {
            addMasterUser(user, chatId);
        } else {
            updateMasterUser(user, active, chatId);
        }
    }

    public MasterUserKey selectMasterUser(String user) {
        return handle(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("select id,chat_id from " + MASTER_USER + " where login=?")) {
                statement.setString(1, user);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        long id = resultSet.getLong(1);
                        long chatId = resultSet.getLong(2);
                        return new MasterUserKey(id, username, chatId);
                    }
                }
                return new MasterUserKey();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public List<MasterUserKey> getAllMasterUsers() {
        return handle(connection -> {
            List<MasterUserKey> list = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("select id,chat_id,login from " + MASTER_USER + "")) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        long id = resultSet.getLong(1);
                        long chatId = resultSet.getLong(2);
                        String login = resultSet.getString(3);
                        list.add(new MasterUserKey(id, login, chatId));
                    }
                }
                return list;
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private void addMasterUser(String user, long chatId) {
        handle(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("insert into " + MASTER_USER + " (id,login,active,chat_id,created) values(?,?,?,?,?)")) {
                statement.setLong(1, masterUserId.getAndIncrement());
                statement.setString(2, user);
                statement.setBoolean(3, true);
                statement.setLong(4, chatId);
                statement.setTimestamp(5, new Timestamp(new Date().getTime()));
                statement.execute();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private void updateMasterUser(String user, boolean active, long chatId) {
        handle(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("update " + MASTER_USER + " set active=?,chat_id=? where login=?")) {
                statement.setBoolean(1, active);
                statement.setLong(2, chatId);
                statement.setString(3, user);
                statement.execute();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public void addUser(String user, String userName, String phone, long chatId) {
        handle(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("insert into " + USERS + " (id,login,name,phone,chat_id,created) values(?,?,?,?,?,?)")) {
                statement.setLong(1, userId.getAndIncrement());
                statement.setString(2, user);
                statement.setString(3, userName);
                statement.setString(4, phone);
                statement.setLong(5, chatId);
                statement.setTimestamp(6, new Timestamp(new Date().getTime()));
                statement.execute();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public User getUserByChatId(long chatId) {
        return handle(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("select id,login from " + USERS + " where chat_id=?")) {
                statement.setLong(1, chatId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        Long userId = resultSet.getLong(1);
                        String login = resultSet.getString(2);
                        return new User(userId, login);
                    }
                }
                return null;
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public List<Order> getActiveOrdersOfTheUser(long chatId) {
        return handle(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("select " + ORDERS_COLUMNS + " from " + ORDERS + " o inner join " + USER_ORDERS + " uo on o.user_id=uo.user_id and o.order_id=uo.id inner join " + USERS + " u on o.user_id=u.id where u.chat_id=? and o.active=? and uo.finished=? and uo.active=?")) {
                statement.setLong(1, chatId);
                statement.setBoolean(2, true);
                statement.setBoolean(3, false);
                statement.setBoolean(4, true);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return ordersMapper(resultSet);
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public UserOrder order(Long userId, List<Order> orderList) {
        UserOrder userOrder = userOrder(new UserOrder(userId).setPrice(PriceUtils.getSum(orderList, configs.getPriceMap())));
        List<Order> activeOrders = getOrders(userId, userOrder.getId());
        Map<String, Order> activeOrdersMap = activeOrders.stream().collect(Collectors.toMap(Order::getName, o -> o));
        for (Order order : orderList) {
            Order orderDb = activeOrdersMap.get(order.getName());
            if (orderDb == null) {
                addOrder(order.setOrderId(userOrder.getId()));
            } else {
                updateOrder(orderDb.update(order).setOrderId(userOrder.getId()));
            }
        }
        Set<String> nameSet = orderList.stream().map(Order::getName).collect(Collectors.toSet());
        for (Entry<String, Order> entry : activeOrdersMap.entrySet()) {
            if (!nameSet.contains(entry.getKey())) {
                Order order = entry.getValue();
                order.setActive(false);
                updateOrder(order.setOrderId(userOrder.getId()));
            }
        }
        return userOrder;
    }

    private void updateOrder(Order order) {
        handle(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("update " + ORDERS + " set order_id=?,name=?,price=?,quantity=?,active=? where id=?")) {
                statement.setLong(1, order.getOrderId());
                statement.setString(2, order.getName());
                statement.setLong(3, order.getPrice());
                statement.setInt(4, order.getQuantity());
                statement.setBoolean(5, order.isActive());
                statement.setLong(6, order.getId());
                statement.execute();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private void addOrder(Order order) {
        handle(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("insert into " + ORDERS + " (id,user_id,order_id,name,price,quantity,created,active) values(?,?,?,?,?,?,?,?)")) {
                statement.setLong(1, orderId.getAndIncrement());
                statement.setLong(2, order.getUserId());
                statement.setLong(3, order.getOrderId());
                statement.setString(4, order.getName());
                statement.setLong(5, order.getPrice());
                statement.setInt(6, order.getQuantity());
                statement.setTimestamp(7, new Timestamp(new Date().getTime()));
                statement.setBoolean(8, true);
                statement.execute();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public UserOrder userOrder(UserOrder userOrder) {
        UserOrder activeUserOrder = getActiveUserOrder(userOrder.getUserId());
        if (activeUserOrder == null) {
            return addUserOrder(userOrder);
        } else {
            return updateUserOrder(activeUserOrder.update(userOrder));
        }
    }

    public void cancelUserOrder(UserOrder userOrder) {
        updateUserOrder(userOrder.setActive(false));
    }

    private UserOrder updateUserOrder(UserOrder userOrder) {
        return handle(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("update " + USER_ORDERS + " set finished=?,active=? where id=?")) {
                statement.setBoolean(1, userOrder.isFinished());
                statement.setBoolean(2, userOrder.isActive());
                statement.setLong(3, userOrder.getId());
                statement.execute();
                return userOrder;
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private UserOrder addUserOrder(UserOrder userOrder) {
        return handle(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("insert into " + USER_ORDERS + " (id,user_id,created,finished,active) values(?,?,?,?,?)")) {
                long id = userOrderId.getAndIncrement();
                Date created = new Date();
                statement.setLong(1, id);
                statement.setLong(2, userOrder.getUserId());
                statement.setTimestamp(3, new Timestamp(created.getTime()));
                statement.setBoolean(4, false);
                statement.setBoolean(5, true);
                statement.execute();
                return new UserOrder(id, userOrder.getUserId(), created, userOrder.getPrice(), false, true);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public UserOrder getActiveUserOrder(Long userId) {
        return handle(connection -> {
            List<Order> list = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("select id,created,price,finished,active from " + USER_ORDERS + " o where o.user_id=? and o.finished=? and o.active=?")) {
                statement.setLong(1, userId);
                statement.setBoolean(2, false);
                statement.setBoolean(3, true);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        long id = resultSet.getLong(1);
                        Date created = new Date(resultSet.getTimestamp(2).getTime());
                        long price = resultSet.getLong(3);
                        boolean finished = resultSet.getBoolean(4);
                        boolean active = resultSet.getBoolean(5);
                        return new UserOrder(id, userId, created, price, finished, active);
                    }
                }
                return null;
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public List<Order> getOrders(Long userId, Long orderId) {
        return handle(connection -> {
            List<Order> list = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("select " + ORDERS_COLUMNS + " from " + ORDERS + " o inner join " + USER_ORDERS + " uo on o.user_id=uo.user_id and o.order_id=uo.id where o.user_id=? and uo.finished=? and uo.active=?")) {
                statement.setLong(1, userId);
                statement.setBoolean(2, false);
                statement.setBoolean(3, true);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return ordersMapper(resultSet);
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private List<Order> ordersMapper(final ResultSet resultSet) throws SQLException {
        List<Order> list = new ArrayList<>();
        while (resultSet.next()) {
            long id = resultSet.getLong(1);
            long userId = resultSet.getLong(2);
            String name = resultSet.getString(3);
            long price = resultSet.getLong(4);
            int quantity = resultSet.getInt(5);
            Date created = new Date(resultSet.getTimestamp(6).getTime());
            boolean finished = resultSet.getBoolean(7);
            boolean active = resultSet.getBoolean(8);
            list.add(new Order(id, userId, name, price, quantity, created, finished, active));
        }
        return list;
    }

    public Long addPayment(UserOrder userOrder, PaymentBean payment) {
        return handle(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("insert into " + PAYMENTS + " (id,order_id,payment_id,idempotence_key,description,price,created,status,active,response) values(?,?,?,?,?,?,?,?,?,?)")) {
                long id = paymentId.getAndIncrement();
                statement.setLong(1, id);
                statement.setLong(2, userOrder.getId());
                statement.setString(3, payment.getPaymentId());
                statement.setString(4, payment.getIdempotenceKey());
                statement.setString(5, payment.getDescription());
                statement.setLong(6, userOrder.getPrice());
                statement.setTimestamp(7, new Timestamp(new Date().getTime()));
                statement.setString(8, payment.getStatus());
                statement.setBoolean(9, true);
                statement.setString(10, payment.getResponse());
                statement.execute();
                return id;
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public List<Payment> getActivePayments(Long orderId) {
        return handle(connection -> {
            List<Payment> list = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("select id,payment_id,idempotence_key,description,price,created,status,response from " + PAYMENTS + " where order_id=? and active=?")) {
                statement.setLong(1, orderId);
                statement.setBoolean(2, true);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        long id = resultSet.getLong(1);
                        String paymentId = resultSet.getString(2);
                        String idempotenceKey = resultSet.getString(3);
                        String description = resultSet.getString(4);
                        long price = resultSet.getLong(5);
                        Date created = new Date(resultSet.getTimestamp(6).getTime());
                        String status = resultSet.getString(7);
                        String response = resultSet.getString(8);
                        list.add(new Payment(id, orderId, paymentId, idempotenceKey, description, price, created, true, status, response));
                    }
                    return list;
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public void checkActivePayments() {
        handle(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("select id,payment_id,idempotence_key,description,price,created,response from " + PAYMENTS + " where active=? and status=?")) {
                statement.setBoolean(1, true);
                statement.setString(2, "pending");
                try (ResultSet resultSet = statement.executeQuery()) {
                 {  "id" : "30e76794-000f-5001-9000-133422f807d0",  "status" : "succeeded",  "amount" : {    "value" : "800.00",    "currency" : "RUB"  },  "income_amount" : {    "value" : "772.00",    "currency" : "RUB"  },  "description" : "Заказ № 1",  "recipient" : {    "account_id" : "1226693",    "gateway_id" : "2595659"  },  "payment_method" : {    "type" : "bank_card",    "id" : "30e76794-000f-5001-9000-133422f807d0",    "saved" : false,    "status" : "inactive",    "title" : "Bank card *4477",    "card" : {      "first6" : "555555",      "last4" : "4477",      "expiry_year" : "2030",      "expiry_month" : "12",      "card_type" : "MasterCard",      "card_product" : {        "code" : "E"      },      "issuer_country" : "US"    }  },  "captured_at" : "2025-12-31T16:55:19.514Z",  "created_at" : "2025-12-31T16:48:20.501Z",  "test" : true,  "refunded_amount" : {    "value" : "0.00",    "currency" : "RUB"  },  "paid" : true,  "refundable" : true,  "metadata" : { },  "authorization_details" : {    "rrn" : "496046895097022",    "auth_code" : "857156",    "three_d_secure" : {      "applied" : true,      "protocol" : "v1",      "method_completed" : false,      "challenge_completed" : true    }  }}
   while (resultSet.next()) {
                        long id = resultSet.getLong(1);
                        String paymentId = resultSet.getString(2);
                        String idempotenceKey = resultSet.getString(3);
                        String description = resultSet.getString(4);
                        long price = resultSet.getLong(5);
                        Date created = new Date(resultSet.getTimestamp(6).getTime());
                        String response = resultSet.getString(7);
                        yookassa.checkPayment(paymentId);
                    }
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public void cancelPayment(String idempotenceKey, String cancelResponse) {
        handle(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("update " + PAYMENTS + " set active=?,cancel_response=? where idempotence_key=?")) {
                statement.setBoolean(1, false);
                statement.setString(2, cancelResponse);
                statement.setString(3, idempotenceKey);
                statement.execute();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }
}
