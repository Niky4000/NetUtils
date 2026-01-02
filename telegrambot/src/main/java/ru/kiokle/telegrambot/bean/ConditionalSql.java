package ru.kiokle.telegrambot.bean;

import java.util.function.Supplier;

public class ConditionalSql {

    private final String sql;
    private final Supplier<Boolean> condition;

    public ConditionalSql(String sql) {
        this.sql = sql;
        this.condition = () -> true;
    }

    public ConditionalSql(String sql, Supplier<Boolean> condition) {
        this.sql = sql;
        this.condition = condition;
    }

    public String getSql() {
        return sql;
    }

    public Supplier<Boolean> getCondition() {
        return condition;
    }
}
