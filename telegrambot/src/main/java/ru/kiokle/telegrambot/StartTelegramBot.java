package ru.kiokle.telegrambot;

import ru.kiokle.telegrambot.bean.PaymentBean;

/**
 * @author me
 */
public class StartTelegramBot {

    public static void main(String[] args) throws Exception {
//        ConcurrentHashMap<String, AtomicInteger> concurrentHashMap = new ConcurrentHashMap<>();
//        concurrentHashMap.put("Hello", new AtomicInteger(0));
//        concurrentHashMap.computeIfPresent("Hello", (k, v) -> null);

//        String str = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(List.of(new OrderBean("/home/me/Булки/bread.jpg", "Булка классическая белая\n150 руб", "bread", "булочка"), new OrderBean("/home/me/Булки/cookies.png", "Овсяные печеньки с корицей\n50 руб", "cookies", "печенька")));
        new JavaTelegramBotApi().start(args);
//        TelegramBots.start(args);
//        new Yookassa().testServer();
//        new Yookassa().createPayment();
//        new Payment("{  \"id\" : \"30e63953-000f-5001-9000-18e800ed4d95\",  \"status\" : \"pending\",  \"amount\" : {    \"value\" : \"100.00\",    \"currency\" : \"RUB\"  },  \"description\" : \"Заказ №2\",  \"recipient\" : {    \"account_id\" : \"1226693\",    \"gateway_id\" : \"2595659\"  },  \"created_at\" : \"2025-12-30T19:18:43.154Z\",  \"confirmation\" : {    \"type\" : \"redirect\",    \"confirmation_url\" : \"https://yoomoney.ru/checkout/payments/v2/contract?orderId=30e63953-000f-5001-9000-18e800ed4d95\"  },  \"test\" : true,  \"paid\" : false,  \"refundable\" : false,  \"metadata\" : { }}");
//        new PaymentBean("{  \"id\" : \"30ea14c1-000f-5001-9000-16bfe7151750\",  \"status\" : \"succeeded\",  \"amount\" : {    \"value\" : \"1300.00\",    \"currency\" : \"RUB\"  },  \"income_amount\" : {    \"value\" : \"1244.49\",    \"currency\" : \"RUB\"  },  \"description\" : \"Заказ № 1\",  \"recipient\" : {    \"account_id\" : \"1226693\",    \"gateway_id\" : \"2595659\"  },  \"payment_method\" : {    \"type\" : \"bank_card\",    \"id\" : \"30ea14c1-000f-5001-9000-16bfe7151750\",    \"saved\" : false,    \"status\" : \"inactive\",    \"title\" : \"Bank card *4477\",    \"card\" : {      \"first6\" : \"555555\",      \"last4\" : \"4477\",      \"expiry_year\" : \"2033\",      \"expiry_month\" : \"12\",      \"card_type\" : \"MasterCard\",      \"card_product\" : {        \"code\" : \"E\"      },      \"issuer_country\" : \"US\"    }  },  \"captured_at\" : \"2026-01-02T17:32:08.977Z\",  \"created_at\" : \"2026-01-02T17:31:45.279Z\",  \"test\" : true,  \"refunded_amount\" : {    \"value\" : \"0.00\",    \"currency\" : \"RUB\"  },  \"paid\" : true,  \"refundable\" : true,  \"metadata\" : { },  \"authorization_details\" : {    \"rrn\" : \"814565143457370\",    \"auth_code\" : \"624562\",    \"three_d_secure\" : {      \"applied\" : true,      \"protocol\" : \"v1\",      \"method_completed\" : false,      \"challenge_completed\" : true    }  }}", "4444");
    }
}
