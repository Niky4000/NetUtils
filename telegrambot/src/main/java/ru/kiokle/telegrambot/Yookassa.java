package ru.kiokle.telegrambot;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import ru.kiokle.telegrambot.bean.PaymentBean;
import ru.kiokle.telegrambot.db.bean.UserOrder;

public class Yookassa {

    private final String yookassaCreatePaymentUrl;
    private final String yookassaCheckPaymentUrl;
    private final String yookassaCancelPaymentUrl;
    private final String credentials;
    private final String returnUrl;
    private final H2 h2;

    public Yookassa(FileUtils fileUtils, H2 h2) {
        this.h2 = h2;
        Properties systemProperties = fileUtils.getSystemProperties();
        yookassaCreatePaymentUrl = (String) systemProperties.get("yookassaCreatePaymentUrl");
        yookassaCheckPaymentUrl = (String) systemProperties.get("yookassaCheckPaymentUrl");
        yookassaCancelPaymentUrl = (String) systemProperties.get("yookassaCancelPaymentUrl");
        credentials = (String) systemProperties.get("credentials");
        returnUrl = (String) systemProperties.get("returnUrl");
    }

    public void testServer() throws Exception {
        ServerSocket serverSocket = new ServerSocket(8080);
        while (true) {
            Socket socket = serverSocket.accept();
            try (InputStream inputStream = socket.getInputStream()) {
                String string = new String(inputStream.readAllBytes());
                System.out.println(string);
            }
        }
    }

    public PaymentBean createPayment(String idempotenceKey, UserOrder userOrder) {
        try {
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Authorization", "Basic " + r(credentials));
            headers.put("Idempotence-Key", idempotenceKey);
            headers.put("Content-Type", "application/json");
            String sendPostHttps = sendPostHttps(yookassaCreatePaymentUrl, RequestMethod.POST, headers, "{\n"
                    + "        \"amount\": {\n"
                    + "          \"value\": \"" + userOrder.getPrice() + ".00\",\n"
                    + "          \"currency\": \"RUB\"\n"
                    + "        },\n"
                    + "        \"capture\": true,\n"
                    + "        \"confirmation\": {\n"
                    + "          \"type\": \"redirect\",\n"
                    + "          \"return_url\": \"" + returnUrl + "\"\n"
                    + "        },\n"
                    + "        \"description\": \"Заказ № " + userOrder.getId() + "\"\n"
                    + "      }");
//            System.out.println(sendPostHttps);
            PaymentBean payment = new PaymentBean(sendPostHttps, idempotenceKey);
            Long paymentId = h2.addPayment(userOrder, payment);
            return payment.setId(paymentId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public PaymentBean checkPayment(String paymentId) {
        try {
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Authorization", "Basic " + r(credentials));
            headers.put("Content-Type", "application/json");
            String sendPostHttps = sendPostHttps(yookassaCheckPaymentUrl.replace("{payment_id}", paymentId), RequestMethod.GET, headers, null);
//            System.out.println(sendPostHttps);
            PaymentBean payment = new PaymentBean(sendPostHttps, null);
            return payment;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void cancelPayment(String paymentId, String idempotenceKey) {
        try {
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Authorization", "Basic " + r(credentials));
            headers.put("Idempotence-Key", idempotenceKey);
            headers.put("Content-Type", "application/json");
            String sendPostHttps = sendPostHttps(yookassaCancelPaymentUrl.replace("{payment_id}", paymentId), RequestMethod.POST, headers, "{}");
            System.out.println(sendPostHttps);
            h2.cancelPayment(idempotenceKey, sendPostHttps);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final String USER_AGENT = "Apache-HttpClient/4.1.1 (java 1.5)";
    private static final int READ_TIMEOUT = 60000;

    private enum RequestMethod {
        GET, POST
    }

    @SuppressWarnings("all")
    private String sendHttpRequest(String urlStr, RequestMethod requestMethod, Map<String, String> headers, String urlParameters) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setReadTimeout(READ_TIMEOUT);
        con.setRequestMethod(requestMethod.name());
        headers.entrySet().forEach(entry -> con.setRequestProperty(entry.getKey(), entry.getValue()));
        con.setDoOutput(true);
        if (requestMethod.equals(RequestMethod.POST)) {
            try (OutputStream wr = con.getOutputStream()) {
                wr.write(urlParameters.getBytes());
                wr.flush();
            }
        }
        int responseCode = con.getResponseCode();
        InputStream errorStream = getErrorStream(con);
        try (InputStream inputStream = (errorStream != null ? errorStream : getInputStream(con))) {
            StringBuffer response = readResponse(con, inputStream);
            if (responseCode != java.net.HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Response code = " + responseCode + "!");
            }
            return response.toString();
        }
    }

    private InputStream getInputStream(HttpURLConnection con) {
        try {
            return con.getInputStream();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private InputStream getErrorStream(HttpURLConnection con) {
        return con.getErrorStream();
    }

    private StringBuffer readResponse(HttpURLConnection con, InputStream inputStream) throws IOException {
        StringBuffer response = new StringBuffer();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        return response;
    }

    // HTTP POST request
    @SuppressWarnings("all")
    private String sendPostHttps(String urlStr, RequestMethod requestMethod, Map<String, String> headers, String urlParameters) throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }

            }
        };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        URL url = new URL(urlStr);
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setReadTimeout(READ_TIMEOUT);
        con.setRequestMethod(requestMethod.name());

        headers.entrySet().forEach(entry -> con.setRequestProperty(entry.getKey(), entry.getValue()));
        con.setDoOutput(true);
        if (requestMethod.equals(RequestMethod.POST)) {
            try (OutputStream wr = con.getOutputStream()) {
                wr.write(urlParameters.getBytes());
                wr.flush();
            }
        }
        int responseCode = con.getResponseCode();
        InputStream errorStream = getErrorStream(con);
        try (InputStream inputStream = (errorStream != null ? errorStream : getInputStream(con))) {
            StringBuffer response = readResponse(con, inputStream);
            if (responseCode != java.net.HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Response code = " + responseCode + "!");
            }
            return response.toString();
        }
    }

    private static String r(String str) {
//        return URLEncoder.encode(str, Charset.forName("utf-8"));
        return Base64.getEncoder().encodeToString(str.getBytes());
    }

}
