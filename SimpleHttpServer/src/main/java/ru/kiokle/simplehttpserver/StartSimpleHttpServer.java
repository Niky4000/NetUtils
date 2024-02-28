package ru.kiokle.simplehttpserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import ru.kiokle.simplehttpserver.clients.ExecCommandClient;
import ru.kiokle.simplehttpserver.clients.PingClient;
import ru.kiokle.simplehttpserver.clients.UploadClient;
import ru.kiokle.simplehttpserver.handlers.CommandEnum;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.EXEC;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.LENGTH;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.UPLOAD;
import ru.kiokle.simplehttpserver.handlers.CommandHandler;
import static ru.kiokle.simplehttpserver.handlers.CommandHandler.makeStandartOutput;
import ru.kiokle.simplehttpserver.handlers.ExecCommandHandler;
import ru.kiokle.simplehttpserver.handlers.UploadCommandHandler;
import ru.kiokle.simplehttpserver.utils.MapBuilder;

public class StartSimpleHttpServer {

    public static final int BUFFER_SIZE = 1024 * 1024;

    public static void main(String[] args) throws Exception {
        List<String> argList = Stream.of(args).collect(Collectors.toList());
        if (argList.contains("-client")) {
            // java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=21044 -jar /home/me/GIT/NetUtils/SimpleHttpServer/target/SimpleHttpServer.jar -client http://mgfomi116.i2p/?i2paddresshelper=4x37bsomt3n5oo3mx4a3u3h2asp44mpzqstvke6ctxwlz5qqbkna.b32.i2p -port 80 -proxyPort 4444
            String client = getConfig("-client", argList);
            String host = getConfig("-host", argList);
            Integer port = Integer.valueOf(getConfig("-port", argList));
            Integer proxyPort = Integer.valueOf(getConfig("-proxyPort", argList));
            if (client.equals("ping")) {
                // java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=21044 -jar /home/me/GIT/NetUtils/SimpleHttpServer/target/SimpleHttpServer.jar -client ping -host mgfomi116.i2p -port 80 -proxyPort 4444
                // java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=21044 -jar /home/me/GIT/NetUtils/SimpleHttpServer/target/SimpleHttpServer.jar -client ping -host me-virtual2.i2p -port 80 -proxyPort 4444
                new I2PClient().connect(host, port, proxyPort, new PingClient(argList, host, port, proxyPort));
            } else if (client.equals("exec")) {
                // java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=21044 -jar /home/me/GIT/NetUtils/SimpleHttpServer/target/SimpleHttpServer.jar -client ping -host me-virtual2.i2p -port 80 -proxyPort 4444 -command "ls -a /home/me/Distributives"
                new I2PClient().connect(host, port, proxyPort, new ExecCommandClient(argList, host, port, proxyPort));
            } else if (client.equals("upload")) {
                new I2PClient().connect(host, port, proxyPort, new UploadClient(argList, host, port, proxyPort));
            }
//            new I2PClient().createServer();
        } else {
            Integer port = Integer.valueOf(getConfig("-port", argList));
            new StartSimpleHttpServer().startHttpServer(port);
        }
    }

    private volatile boolean stop = false;
    private Integer port;

    public void startHttpServer(Integer port) throws IOException {
        this.port = port;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    if (stop) {
                        break;
                    }
                    handleSocket(socket);
                } catch (Exception e) {
                    String ex = e.getMessage();
                }
            }
        }
    }

    public void stopHttpServer() throws IOException {
        stop = true;
        try (Socket socket = new Socket("127.0.0.1", port)) {
        }
    }

    Map<CommandEnum, Supplier<CommandHandler>> commandHandlerMap = MapBuilder.<CommandEnum, Supplier<CommandHandler>>builder().put(UPLOAD, () -> new UploadCommandHandler()).put(EXEC, () -> new ExecCommandHandler()).build();
    Set<String> allPossibleCommandSet = commandHandlerMap.keySet().stream().map(Enum::name).collect(Collectors.toSet());
    int headLength = 16696;

    private void handleSocket(Socket socket) throws IOException {
        try (BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE);
                BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE);) {
            readInputStream(inputStream, outputStream);
        } finally {
            socket.close();
        }
    }

    private void readInputStream(BufferedInputStream inputStream, BufferedOutputStream outputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int read = 0;
        do {
            byte[] buffer = new byte[BUFFER_SIZE];
            do {
                int available = inputStream.available();
                System.out.println("available = " + available);
                if (available <= 0) {
                    break;
                }
                read = inputStream.read(buffer);
                byteArrayOutputStream.write(buffer, 0, read);
            } while (read > 0);
            Entry<String, Integer> headEntry = getHead(byteArrayOutputStream.toByteArray());
            if (headEntry == null) {
                makeStandartOutput(outputStream);
                break;
            }
            String head = headEntry.getKey();
            int headIndex = headEntry.getValue();
            Entry<CommandEnum, String> command = getCommand(head);
            if (command == null) {
                break;
            }
            int length = getLength(head);
            if (length < 0) {
                break;
            }
            commandHandlerMap.get(command.getKey()).get().handle(byteArrayOutputStream, inputStream, length, outputStream, headIndex, command.getValue());
            if (read < BUFFER_SIZE) {
                break;
            }
        } while (read > 0);
    }

    public static final String endStr = "\n";
    public static final String endStr2 = "\r";
    public static final byte[] end = endStr.getBytes();
    public static final String headEndStr = "\n\n";
    public static final byte[] headEnd = headEndStr.getBytes();
    public static final String delimiter = " ";

    private Entry<String, Integer> getHead(byte[] input) {
        return substringAfter(input, 0, headEnd);
    }

    private Entry<CommandEnum, String> getCommand(String head) {
        String[] split = head.split(endStr);
        return Stream.of(split).filter(s -> s.contains(delimiter)).map(s -> {
            String key = r(s.substring(0, s.indexOf(delimiter)));
            String value = r(s.substring(s.indexOf(delimiter) + delimiter.length()));
            return new AbstractMap.SimpleEntry<>(key, value);
        }).filter(s -> {
            return allPossibleCommandSet.contains(s.getKey());
        }).findFirst().map(s -> new AbstractMap.SimpleEntry<>(CommandEnum.valueOf(s.getKey()), s.getValue())).orElse(null);
    }

    private String r(String s) {
        return s.replace(endStr, "").replace(endStr2, "");
    }

    private int getLength(String head) {
        return Stream.of(head.split(endStr)).filter(s -> s.startsWith(LENGTH.name() + delimiter)).findFirst().map(s -> s.substring(LENGTH.name().length() + delimiter.length(), s.length())).map(Integer::valueOf).orElse(-1);
    }

    private Entry<String, Integer> substringAfter(byte[] bytes, int startIndex, byte[] end) {
        int index = contains(bytes, end, startIndex);
        if (index > -1) {
            return new AbstractMap.SimpleEntry<>(new String(bytes, startIndex, index), index + end.length);
        }
        return null;
    }

    private int contains(byte[] bytes, byte[] array, int startIndex) {
        for (int i = 0; i < bytes.length; i++) {
            boolean equal = true;
            for (int j = 0; j < array.length; j++) {
                if (i + j >= bytes.length || bytes[i + j] != array[j]) {
                    equal = false;
                    break;
                }
            }
            if (equal) {
                return i;
            }
        }
        return -1;
    }

    public static String getConfig(String arg, List<String> argList) {
        int indexOf = argList.indexOf(arg);
        if (indexOf >= 0) {
            return argList.get(indexOf + 1);
        } else {
            return null;
        }
    }
}
