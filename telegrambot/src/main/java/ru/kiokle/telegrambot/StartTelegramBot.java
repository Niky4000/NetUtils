package ru.kiokle.telegrambot;

/**
 * @author me
 */
public class StartTelegramBot {

    public static void main(String[] args) {
//        ConcurrentHashMap<String, AtomicInteger> concurrentHashMap = new ConcurrentHashMap<>();
//        concurrentHashMap.put("Hello", new AtomicInteger(0));
//        concurrentHashMap.computeIfPresent("Hello", (k, v) -> null);
        JavaTelegramBotApi.start(args);
//        TelegramBots.start(args);
    }
}
