package ru.kiokle.telegrambot.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import ru.kiokle.telegrambot.bean.ConditionalSql;

public class DatabaseUpdates {

    private static final String getVersionSQL = "select version from database_version";

    public void update(Connection connection) throws SQLException {
        AtomicLong databaseVersion = new AtomicLong(0L);
        sql(connection, List.of(
                new ConditionalSql("create table database_version (version long, primary key(version))", () -> {
                    databaseVersion.set(getVersion(connection));
                    return databaseVersion.compareAndSet(-1L, 0L);
                }),
                new ConditionalSql("create table users (id long, login varchar(255), name varchar(255), phone varchar(11), chat_id long, created timestamp, primary key(id))", () -> databaseVersion.get() == 0L),
                new ConditionalSql("create index users_id_index on users (id)", () -> databaseVersion.get() == 0L),
                new ConditionalSql("create index users_chat_id_index on users (chat_id)", () -> databaseVersion.get() == 0L),
                new ConditionalSql("create table user_orders (id long, user_id long, created timestamp, price long, finished boolean, paid boolean, active boolean, primary key(id), foreign key (user_id) references users (id))", () -> databaseVersion.get() == 0L),
                new ConditionalSql("create index user_orders_id_index on user_orders (id)", () -> databaseVersion.get() == 0L),
                new ConditionalSql("create index user_orders_user_id_index on user_orders (user_id)", () -> databaseVersion.get() == 0L),
                new ConditionalSql("create table orders (id long, user_id long, order_id long, name varchar(255), price long, quantity int, created timestamp, active boolean, primary key(id), foreign key (user_id) references users (id), foreign key (order_id) references orders (id))", () -> databaseVersion.get() == 0L),
                new ConditionalSql("create index orders_id_index on orders (id)", () -> databaseVersion.get() == 0L),
                new ConditionalSql("create index orders_user_id_index on orders (user_id)", () -> databaseVersion.get() == 0L),
                new ConditionalSql("create index orders_order_id_index on orders (order_id)", () -> databaseVersion.get() == 0L),
                new ConditionalSql("create table payments (id long, order_id long, payment_id varchar(36), idempotence_key varchar(32), description varchar(255), price long, created timestamp, status varchar(32), active boolean, primary key(id), foreign key (order_id) references user_orders (id))", () -> databaseVersion.get() == 0L),
                new ConditionalSql("create index payments_id_index on payments (id)", () -> databaseVersion.get() == 0L),
                new ConditionalSql("create index payments_order_id_index on payments (order_id)", () -> databaseVersion.get() == 0L),
                new ConditionalSql("create table payment_responses (id long, order_id long, payment_id long, created timestamp, status varchar(32), response varchar(65536), primary key(id), foreign key (payment_id) references payments (id), foreign key (order_id) references user_orders (id))", () -> databaseVersion.get() == 0L),
                new ConditionalSql("create index payment_responses_id_index on payment_responses (id)", () -> databaseVersion.get() == 0L),
                new ConditionalSql("create index payment_responses_order_id_index on payment_responses (order_id)", () -> databaseVersion.get() == 0L),
                new ConditionalSql("create index payment_responses_payment_id_index on payment_responses (payment_id)", () -> databaseVersion.get() == 0L),
                new ConditionalSql("create table master_user (id long, login varchar(255), active boolean, chat_id long, created timestamp, primary key(id))", () -> databaseVersion.get() == 0L),
                new ConditionalSql("create index master_user_id_index on master_user (id)", () -> databaseVersion.get() == 0L),
                new ConditionalSql("create index master_user_login_index on master_user (login)", () -> databaseVersion.get() == 0L),
                new ConditionalSql("insert into database_version (version) values(1)", () -> databaseVersion.get() == 0L)
        ));
        databaseVersion.compareAndSet(0L, 1L);
    }

    private void sql(Connection connection, List<ConditionalSql> updateList) {
        try {
            for (ConditionalSql sqlAndPredicate : updateList) {
                if (sqlAndPredicate.getCondition().get()) {
                    try (PreparedStatement statement = connection.prepareStatement(sqlAndPredicate.getSql())) {
                        statement.execute();
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean check(Connection connection, String sql) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private long getVersion(Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement(getVersionSQL)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    long version = resultSet.getLong(1);
                    return version;
                } else {
                    return -1L;
                }
            }
        } catch (Exception e) {
            return -1L;
        }
    }
}
