package ru.kiokle.telegrambot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.AbstractMap;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private static final String PAYMENT_RESPONSES = "payment_responses";
    private static final String ORDERS_COLUMNS = "o.id,o.user_id,o.name,o.price,o.quantity,uo.created,uo.finished,o.active";
    private static final String ORDERS_COLUMNS_ONLY = "o.id,o.user_id,o.name,o.price,o.quantity,o.created,o.active";
    private static final String USER_ORDER_COLUMNS = "id,user_id,created,price,finished,paid,active";
    private AtomicLong masterUserId;
    private AtomicLong userId;
    private AtomicLong userOrderId;
    private AtomicLong orderId;
    private AtomicLong paymentId;
    private AtomicLong paymentResponsesId;

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
        paymentResponsesId = getMaxId(PAYMENT_RESPONSES);
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

    public User getUserById(long id) {
        return handle(connection -> {
            return getUserById(connection, id);
        });
    }

    private User getUserById(Connection connection, long id) {
        try (PreparedStatement statement = connection.prepareStatement("select login,chat_id from " + USERS + " where id=?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String login = resultSet.getString(1);
                    Long chatId = resultSet.getLong(2);
                    return new User(id, login, chatId);
                }
            }
            return null;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<Order> getActiveOrders() {
        return handle(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("select " + ORDERS_COLUMNS + " from " + ORDERS + " o inner join " + USER_ORDERS + " uo on o.user_id=uo.user_id and o.order_id=uo.id inner join " + USERS + " u on o.user_id=u.id where o.active=? and uo.finished=? and uo.active=? order by uo.created asc,u.created asc")) {
                statement.setBoolean(1, true);
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

    public List<Order> getOrdersByUserOrderId(Long orderId) {
        return handle(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("select " + ORDERS_COLUMNS_ONLY + " from " + ORDERS + " o where o.order_id=? order by o.created asc")) {
                statement.setLong(1, orderId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return ordersOnlyMapper(resultSet);
                }
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

    public UserOrder updateUserOrder(UserOrder userOrder) {
        return handle(connection -> {
            return updateUserOrder(connection, userOrder);
        });
    }

    private UserOrder updateUserOrder(Connection connection, UserOrder userOrder) throws RuntimeException {
        try (PreparedStatement statement = connection.prepareStatement("update " + USER_ORDERS + " set price=?,finished=?,paid=?,active=? where id=?")) {
            statement.setLong(1, userOrder.getPrice());
            statement.setBoolean(2, userOrder.isFinished());
            statement.setBoolean(3, userOrder.isPaid());
            statement.setBoolean(4, userOrder.isActive());
            statement.setLong(5, userOrder.getId());
            statement.execute();
            return userOrder;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private UserOrder addUserOrder(UserOrder userOrder) {
        return handle(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("insert into " + USER_ORDERS + " (id,user_id,price,created,finished,active) values(?,?,?,?,?,?)")) {
                long id = userOrderId.getAndIncrement();
                Date created = new Date();
                statement.setLong(1, id);
                statement.setLong(2, userOrder.getUserId());
                statement.setLong(3, userOrder.getPrice());
                statement.setTimestamp(4, new Timestamp(created.getTime()));
                statement.setBoolean(5, false);
                statement.setBoolean(6, true);
                statement.execute();
                return new UserOrder(id, userOrder.getUserId(), created, userOrder.getPrice(), false, false, true);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public UserOrder getActiveUserOrder(Long userId) {
        return handle(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("select " + USER_ORDER_COLUMNS + " from " + USER_ORDERS + " o where o.user_id=? and o.finished=? and o.active=?")) {
                statement.setLong(1, userId);
                statement.setBoolean(2, false);
                statement.setBoolean(3, true);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return userOrderMapper(resultSet);
                    }
                }
                return null;
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public List<UserOrder> getPaidUserOrder() {
        return handle(connection -> {
            List<UserOrder> list = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("select " + USER_ORDER_COLUMNS + " from " + USER_ORDERS + " o where o.finished=? and o.paid=? and o.active=?")) {
                statement.setBoolean(1, false);
                statement.setBoolean(2, true);
                statement.setBoolean(3, true);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        list.add(userOrderMapper(resultSet));
                    }
                }
                return list;
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public UserOrder getUserOrder(Long id) {
        return handle(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("select " + USER_ORDER_COLUMNS + " from " + USER_ORDERS + " o where o.id=?")) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return userOrderMapper(resultSet);
                    }
                }
                return null;
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public UserOrder getUserOrderByUserId(Long id) {
        return handle(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("select " + USER_ORDER_COLUMNS + " from " + USER_ORDERS + " o where o.user_id=?")) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return userOrderMapper(resultSet);
                    }
                }
                return null;
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private UserOrder getUserOrderById(Connection connection, Long id) {
        try (PreparedStatement statement = connection.prepareStatement("select " + USER_ORDER_COLUMNS + " from " + USER_ORDERS + " o where o.id=?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return userOrderMapper(resultSet);
                }
            }
            return null;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private UserOrder userOrderMapper(final ResultSet resultSet) throws SQLException {
        long id = resultSet.getLong(1);
        long userId = resultSet.getLong(2);
        Date created = new Date(resultSet.getTimestamp(3).getTime());
        long price = resultSet.getLong(4);
        boolean finished = resultSet.getBoolean(5);
        boolean paid = resultSet.getBoolean(6);
        boolean active = resultSet.getBoolean(7);
        return new UserOrder(id, userId, created, price, finished, paid, active);
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

    private List<Order> ordersOnlyMapper(final ResultSet resultSet) throws SQLException {
        List<Order> list = new ArrayList<>();
        while (resultSet.next()) {
            long id = resultSet.getLong(1);
            long userId = resultSet.getLong(2);
            String name = resultSet.getString(3);
            long price = resultSet.getLong(4);
            int quantity = resultSet.getInt(5);
            Date created = new Date(resultSet.getTimestamp(6).getTime());
            boolean active = resultSet.getBoolean(7);
            list.add(new Order(id, userId, name, price, quantity, created, false, active));
        }
        return list;
    }

    public Long addPayment(UserOrder userOrder, PaymentBean payment) {
        return handle(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("insert into " + PAYMENTS + " (id,order_id,payment_id,idempotence_key,description,price,created,status,active) values(?,?,?,?,?,?,?,?,?)");) {
                long id = paymentId.getAndIncrement();
                Timestamp created = new Timestamp(new Date().getTime());
                statement.setLong(1, id);
                statement.setLong(2, userOrder.getId());
                statement.setString(3, payment.getPaymentId());
                statement.setString(4, payment.getIdempotenceKey());
                statement.setString(5, payment.getDescription());
                statement.setLong(6, userOrder.getPrice());
                statement.setTimestamp(7, created);
                statement.setString(8, payment.getStatus());
                statement.setBoolean(9, true);
                statement.execute();
                addPaymentResponse(connection, userOrder.getId(), id, created, payment.getStatus(), payment.getResponse());
                return id;
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private void addPaymentResponse(Connection connection, Long orderId, Long paymentId, Timestamp created, String status, String response) {
        try (PreparedStatement statement2 = connection.prepareStatement("insert into " + PAYMENT_RESPONSES + " (id,order_id,payment_id,created,status,response) values(?,?,?,?,?,?)")) {
            statement2.setLong(1, paymentResponsesId.getAndIncrement());
            statement2.setLong(2, orderId);
            statement2.setLong(3, paymentId);
            statement2.setTimestamp(4, created);
            statement2.setString(5, status);
            statement2.setString(6, response);
            statement2.execute();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<Payment> getActivePayments(Long orderId) {
        return handle(connection -> {
            List<Payment> list = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("select id,payment_id,idempotence_key,description,price,created,status from " + PAYMENTS + " where order_id=? and active=? order by created")) {
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
                        list.add(new Payment(id, orderId, paymentId, idempotenceKey, description, price, created, true, status));
                    }
                    return list;
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public void checkActivePayments(BiConsumer<Entry<User, UserOrder>, PaymentBean> notify) {
        handle(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("select id,order_id,payment_id,idempotence_key,description,price,created from " + PAYMENTS + " where active=? and status=?")) {
                statement.setBoolean(1, true);
                statement.setString(2, "pending");
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        long id = resultSet.getLong(1);
                        long orderId = resultSet.getLong(2);
                        String paymentId = resultSet.getString(3);
                        String idempotenceKey = resultSet.getString(4);
                        String description = resultSet.getString(5);
                        long price = resultSet.getLong(6);
                        Date created = new Date(resultSet.getTimestamp(7).getTime());
                        PaymentBean newPayment = yookassa.checkPayment(paymentId);
                        checkPayment(connection, orderId, newPayment, id, notify);
                    }
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private void checkPayment(Connection connection, Long orderId, PaymentBean newPayment, Long paymentId, BiConsumer<Entry<User, UserOrder>, PaymentBean> notify) {
        if (!"pending".equals(newPayment.getStatus())) {
            addPaymentResponse(connection, orderId, paymentId, new Timestamp(new Date().getTime()), newPayment.getStatus(), newPayment.getResponse());
            updatePaymentStatus(connection, newPayment.getStatus(), paymentId);
            if ("succeeded".equals(newPayment.getStatus())) {
                UserOrder userOrder = getUserOrderById(connection, orderId);
                updateUserOrder(connection, userOrder.setPaid(true));
                User user = getUserById(connection, userOrder.getUserId());
                notify.accept(new AbstractMap.SimpleEntry<>(user, userOrder), newPayment);
            }
        }
    }

    public PaymentBean getFirstPendingPaymentResponse(Long paymentId) {
        return handle(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("select response from " + PAYMENT_RESPONSES + " where payment_id=? and status=?")) {
                statement.setLong(1, paymentId);
                statement.setString(2, "pending");
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        String string = resultSet.getString(1);
                        PaymentBean paymentBean = new PaymentBean(string, null);
                        return paymentBean;
                    }
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            return null;
        });
    }

//    private List<Entry<Long, String>> getLastPaymentIdAndStatus(Connection connection, Long orderId) {
//        List<Entry<Long, String>> list = new ArrayList<>();
//        try (PreparedStatement statement = connection.prepareStatement("select payment_id,status from " + PAYMENT_RESPONSES + " where order_id=? order by created asc")) {
//            statement.setLong(1, orderId);
//            try (ResultSet resultSet = statement.executeQuery()) {
//                while (resultSet.next()) {
//                    long paymentId = resultSet.getLong(1);
//                    Timestamp created = resultSet.getTimestamp(2);
//                    try (PreparedStatement statement2 = connection.prepareStatement("select status from " + PAYMENT_RESPONSES + " where order_id=? and created=?")) {
//                        statement2.setLong(1, orderId);
//                        statement2.setTimestamp(2, created);
//                        try (ResultSet resultSet2 = statement2.executeQuery()) {
//                            if (resultSet2.next()) {
//                                String status = resultSet2.getString(1);
//                                list.add(new AbstractMap.SimpleEntry<>(paymentId, status));
//                            }
//                        }
//                    }
//                }
//            }
//            return list;
//        } catch (SQLException ex) {
//            throw new RuntimeException(ex);
//        }
//    }
    public void updatePaymentStatus(Connection connection, String status, Long id) {
        try (PreparedStatement statement = connection.prepareStatement("update " + PAYMENTS + " set status=? where id=?")) {
            statement.setString(1, status);
            statement.setLong(2, id);
            statement.execute();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
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
