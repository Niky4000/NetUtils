package ru.kiokle.telegrambot.utils;

import java.util.List;
import java.util.Map;
import ru.kiokle.telegrambot.db.bean.Order;

public class PriceUtils {

    public static long getSum(List<Order> orderList, Map<String, Long> priceMap) {
        return orderList.stream().mapToLong(o -> priceMap.get(o.getName()) * o.getQuantity()).sum();
    }

    public static long getSum(String orderName, int quantity, Map<String, Long> priceMap) {
        return priceMap.get(orderName) * quantity;
    }
}
