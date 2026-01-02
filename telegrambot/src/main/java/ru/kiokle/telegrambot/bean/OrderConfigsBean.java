package ru.kiokle.telegrambot.bean;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OrderConfigsBean {

    private List<OrderBean> orders;
    private Map<String, String> map;
    private Map<String, Long> priceMap;

    public OrderConfigsBean(List<OrderBean> orders, Map<String, String> map) {
        this.orders = orders;
        this.map = map;
        this.priceMap = orders.stream().collect(Collectors.toMap(OrderBean::getName, OrderBean::getPrice));
    }

    public List<OrderBean> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderBean> orders) {
        this.orders = orders;
    }

    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(Map<String, String> map) {
        this.map = map;
    }

    public Map<String, Long> getPriceMap() {
        return priceMap;
    }
}
