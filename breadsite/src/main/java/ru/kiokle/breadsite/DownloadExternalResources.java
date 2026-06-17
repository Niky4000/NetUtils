/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kiokle.breadsite;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 * @author me
 */
public class DownloadExternalResources {

    private static String s = FileSystems.getDefault().getSeparator();

    public void download(String externalSite, File fileToGetLinksFrom) throws Exception {
        FileUtils fileUtils = new FileUtils();
        File basePath = new File((String) fileUtils.getConfigs().get("base"));
        List<String> links = getLinks(fileToGetLinksFrom).stream().filter(l -> l.contains("${site_base}") && checkLinkExtension(l)).collect(Collectors.toList());
        for (String link : links) {
            String substring = link.substring(link.indexOf("${site_base}") + "${site_base}".length(), link.length());
            File file = new File(basePath.getAbsolutePath() + s + substring);
            if (!file.exists()) {
                byte[] httpContent = sendPost(handleLink(link, externalSite), RequestMethod.GET, getHeaders(), null);
                Files.write(file.toPath(), httpContent, StandardOpenOption.CREATE_NEW);
            }
        }
    }

    private String handleLink(String link, String externalSite) {
        String substring = link.substring(link.indexOf("${site_base}") + "${site_base}".length(), link.length());
        return externalSite + substring;
    }

    private boolean checkLinkExtension(String link) {
        String extension = link.substring(link.length() - 4, link.length());
        return Pattern.compile("^\\.[a-zA-Z]+$").matcher(extension).matches();
    }

    List<String> getLinks(File fileToGetLinksFrom) throws IOException {
        String content = new String(Files.readAllBytes(fileToGetLinksFrom.toPath()));
        List<String> links = new ArrayList<>();
        getNextLink(content, "href=\"", "\"", links);
        getNextLink(content, "href='", "'", links);
        getNextLink(content, "src=\"", "\"", links);
        getNextLink(content, "src='", "'", links);
        return links;
    }

    private void getNextLink(String content, String startStr, String endStr, List<String> links) {
        String str = "";
        AtomicInteger index = new AtomicInteger(0);
        while (str != null) {
            str = getNextLink(content, index, startStr, endStr);
            if (str != null) {
                links.add(str);
            }
        }
    }

    private String getNextLink(String content, AtomicInteger start, String str, String str2) {
        int indexOf = content.indexOf(str, start.get());
        if (indexOf > 0) {
            int indexOf2 = content.indexOf(str2, indexOf + str.length());
            start.set(indexOf2 + 1);
            return content.substring(indexOf, indexOf2);
        } else {
            return null;
        }
    }

    private static final String USER_AGENT = "Apache-HttpClient/4.1.1 (java 1.5)";
    private static final int READ_TIMEOUT = 60000;

    private enum RequestMethod {
        GET, POST
    }

    // HTTP POST request
    @SuppressWarnings("all")
    private byte[] sendPost(String urlStr, RequestMethod requestMethod, Map<String, String> headers, String urlParameters) throws Exception {
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
            byte[] response = readResponse(inputStream);
            if (responseCode != java.net.HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Response code = " + responseCode + "!");
            }
            return response;
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

    private byte[] readResponse(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024 * 1024];
        try (BufferedInputStream in = new BufferedInputStream(inputStream, 1024 * 1024)) {
            int read = in.read(buffer);
            while (read > 0) {
                byteArrayOutputStream.write(buffer, 0, read);
                read = in.read(buffer);
            }
        }
        return byteArrayOutputStream.toByteArray();
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

    private Map<String, String> getHeaders() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("accept", "*/*");
        map.put("Content-Type", "application/json");
        map.put("Authorization", "Basic ZGV2ZWxvcGVyOkdJY2F1VzdPYlRsMTk4djRYcjlR");
        map.put("User-Agent", USER_AGENT);
        return map;
    }
}
