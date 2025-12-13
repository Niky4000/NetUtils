package ru.kiokle.simplehttpserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import ru.kiokle.simplehttpserver.clients.ExecCommandClient;
import ru.kiokle.simplehttpserver.clients.LogClient;
import ru.kiokle.simplehttpserver.clients.Md5Client;
import ru.kiokle.simplehttpserver.clients.PingClient;
import ru.kiokle.simplehttpserver.clients.SelfPathClient;
import ru.kiokle.simplehttpserver.clients.StopServerClient;
import ru.kiokle.simplehttpserver.clients.UploadClient;
import ru.kiokle.simplehttpserver.clients.enums.ConnectionType;
import static ru.kiokle.simplehttpserver.clients.enums.ConnectionType.I2P;
import static ru.kiokle.simplehttpserver.clients.enums.ConnectionType.LOCAL;
import ru.kiokle.simplehttpserver.handlers.CommandEnum;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.EXEC;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.LENGTH;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.LOG;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.MD5;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.SELF_PATH;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.STOP;
import static ru.kiokle.simplehttpserver.handlers.CommandEnum.UPLOAD;
import ru.kiokle.simplehttpserver.handlers.CommandHandler;
import static ru.kiokle.simplehttpserver.handlers.CommandHandler.makeStandartOutput;
import ru.kiokle.simplehttpserver.handlers.ExecCommandHandler;
import ru.kiokle.simplehttpserver.handlers.LogsCommandHandler;
import ru.kiokle.simplehttpserver.handlers.Md5CommandHandler;
import ru.kiokle.simplehttpserver.handlers.SelfPathCommandHandler;
import ru.kiokle.simplehttpserver.handlers.StopServerCommandHandler;
import ru.kiokle.simplehttpserver.handlers.UploadCommandHandler;
import ru.kiokle.simplehttpserver.log.Logger;
import ru.kiokle.simplehttpserver.utils.FileUtils;
import static ru.kiokle.simplehttpserver.utils.FileUtils.incrementAddress;
import ru.kiokle.simplehttpserver.utils.MapBuilder;
import ru.kiokle.simplehttpserver.utils.WaitUtils;

public class StartSimpleHttpServer {

    public static final int BUFFER_SIZE = 1024 * 10;
    private final Map<CommandEnum, Supplier<CommandHandler>> commandHandlerMap = MapBuilder.<CommandEnum, Supplier<CommandHandler>>builder().put(UPLOAD, () -> new UploadCommandHandler()).put(EXEC, () -> new ExecCommandHandler()).put(MD5, () -> new Md5CommandHandler()).put(SELF_PATH, () -> new SelfPathCommandHandler()).put(LOG, () -> new LogsCommandHandler()).put(STOP, () -> new StopServerCommandHandler()).build();
    Set<String> allPossibleCommandSet = commandHandlerMap.keySet().stream().map(Enum::name).collect(Collectors.toSet());
    private static final int headLength = 16696;
    public static volatile String[] localArgs;

    public static void main(String[] args) throws Exception {
        localArgs = args;
        List<String> argList = Stream.of(args).collect(Collectors.toList());
        if (argList.contains("-client")) {
            // java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=21044 -jar /home/me/GIT/NetUtils/SimpleHttpServer/target/SimpleHttpServer.jar -client me-virtual2.i2p -port 80 -proxyPort 4444
            String client = getConfig("-client", argList);
            String host = getConfig("-host", argList);
            Integer port = Integer.valueOf(getConfig("-port", argList));
            Integer proxyPort = Integer.valueOf(getConfig("-proxyPort", argList));
            boolean localNetwork = argList.contains("-local");
            ConnectionType connectionType = localNetwork ? LOCAL : I2P;
            if (client.equals("ping")) {
                // java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=21044 -jar /home/me/GIT/NetUtils/SimpleHttpServer/target/SimpleHttpServer.jar -client ping -host me-virtual2.i2p -port 80 -proxyPort 4444
                // java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=21044 -jar /home/me/GIT/NetUtils/SimpleHttpServer/target/SimpleHttpServer.jar -client ping -host me-virtual2.i2p -port 80 -proxyPort 4444
                new PingClient(argList, host, port, proxyPort, connectionType).connect();
            } else if (client.equals("exec")) {
                // java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=21044 -jar /home/me/GIT/NetUtils/SimpleHttpServer/target/SimpleHttpServer.jar -client exec -host me-virtual2.i2p -port 80 -proxyPort 4444 -command "ls -a /home/me/Distributives"
                new ExecCommandClient(argList, host, port, proxyPort, connectionType, getConfig("-command", argList)).connect();
            } else if (client.equals("upload")) {
                // java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=21044 -jar /home/me/GIT/NetUtils/SimpleHttpServer/target/SimpleHttpServer.jar -client upload -host me-virtual2.i2p -port 80 -proxyPort 4444 -file /home/me/GIT/NetUtils/HttpTunnel/target/jutil.jar -toFile D:\\jutil.jar
                new UploadClient(argList, host, port, proxyPort, connectionType, new File(getConfig("-file", argList)), getConfig("-toFile", argList)).connect();
            } else if (client.equals("md5")) {
                // java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=21044 -jar /home/me/GIT/NetUtils/SimpleHttpServer/target/SimpleHttpServer.jar -client md5 -host me-virtual2.i2p -port 80 -proxyPort 4444 -file /home/me/tmp/pollResult4
                AtomicReference<String> md5Reference = new AtomicReference<>();
                new Md5Client(argList, host, port, proxyPort, connectionType, md5Reference).connect();
                Logger.log(md5Reference.get());
            } else if (client.equals("selfPath")) {
                // java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=21044 -jar /home/me/GIT/NetUtils/SimpleHttpServer/target/SimpleHttpServer.jar -client selfPath -host me-virtual2.i2p -port 80 -proxyPort 4444
                AtomicReference<String> selfPathReference = new AtomicReference<>();
                new SelfPathClient(argList, host, port, proxyPort, connectionType, selfPathReference).connect();
                Logger.log(selfPathReference.get());
            } else if (client.equals("logs")) {
                // java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=21044 -jar /home/me/GIT/NetUtils/SimpleHttpServer/target/SimpleHttpServer.jar -client logs -host me-virtual2.i2p -port 80 -proxyPort 4444 -count 128
                AtomicReference<String> logsReference = new AtomicReference<>();
                new LogClient(argList, host, port, proxyPort, connectionType, Integer.valueOf(getConfig("-count", argList)), logsReference).connect();
                Logger.log(logsReference.get());
            } else if (client.equals("selfUpdate")) {
                // java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=21044 -jar /home/me/GIT/NetUtils/SimpleHttpServer/target/SimpleHttpServer.jar -client selfUpdate -host me-virtual2.i2p -port 80 -proxyPort 4444
                selfUpdateImpl(argList, host, port, proxyPort, connectionType);
            }
//            new I2PClient().createServer();
        } else {
            // java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=21044 -jar /home/me/GIT/NetUtils/SimpleHttpServer/target/SimpleHttpServer.jar -port 7662
            // java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=21045 -jar /home/me/GIT/NetUtils/SimpleHttpServer/target/SimpleHttpServer_5555.jar -port 7662
            checkForSelfUpdate(argList, l -> {
                while (!stop) {
                    try {
                        new StartSimpleHttpServer().startHttpServer(Integer.valueOf(getConfig("-port", argList)));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        WaitUtils.waitSomeTime(TIME_TO_WAIT);
                        Logger.log("Waiting...");
                        continue;
                    }
                }
            });
        }
    }

    private static void selfUpdateImpl(List<String> argList, String host, Integer port, Integer proxyPort, ConnectionType connectionType) throws Exception {
        AtomicReference<String> selfPathReference = new AtomicReference<>();
        new SelfPathClient(argList, host, port, proxyPort, connectionType, selfPathReference).connect();
        String remoteSelfPath = getRemoteSelfPathFromRemoteCommandLineArgument(selfPathReference.get());
        String newSelfTempFileName = createSelfTempFileName(remoteSelfPath);
        Logger.log(newSelfTempFileName);
        File pathToJar = FileUtils.getPathToJar();
        new UploadClient(argList, host, port, proxyPort, connectionType, pathToJar, newSelfTempFileName).connect();
        String remoteExecCommand = incrementAddress(selfPathReference.get().replace(remoteSelfPath, newSelfTempFileName));
        new ExecCommandClient(argList, host, port, proxyPort, connectionType, remoteExecCommand).connect();
    }

    private static final String JAR = "-jar";

    private static String getRemoteSelfPathFromRemoteCommandLineArgument(String remoteCommandLineArgument) {
        int jarIndex = remoteCommandLineArgument.indexOf(JAR);
        int indexOfSpaceAfterJar = remoteCommandLineArgument.indexOf(delimiter, jarIndex + JAR.length() + delimiter.length());
        String remoteSelfPath = remoteCommandLineArgument.substring(jarIndex + JAR.length() + delimiter.length(), indexOfSpaceAfterJar);
        return remoteSelfPath;
    }

    private static String createSelfTempFileName(String remoteSelfPath) {
        String remoteTempFileName = remoteSelfPath.substring(0, remoteSelfPath.lastIndexOf(POINT)) + TEMP_FILE_MARK + ((int) (Math.random() * 100000)) + remoteSelfPath.substring(remoteSelfPath.lastIndexOf(POINT));
        return remoteTempFileName;
    }

    private static final String POINT = ".";
    private static final String HYPHEN = "-";
    private static final String TEMP_FILE_MARK = "_";
    public static final int TIME_TO_WAIT = 10;

    private static void checkForSelfUpdate(List<String> argList, Consumer<List<String>> argListConsumer) throws Exception {
        File pathToJar = FileUtils.getPathToJar();
        String name = pathToJar.getName();
        if (name.contains(TEMP_FILE_MARK)) {
            try {
                new StopServerClient(argList, "localhost", Integer.valueOf(getConfig("-port", argList))).connect();
            } catch (Exception e) {
                Logger.log("There is no client launched!");
            }
            WaitUtils.waitSomeTime(TIME_TO_WAIT);
            String newName = pathToJar.getParentFile().getAbsolutePath() + File.separator + name.substring(0, name.indexOf(TEMP_FILE_MARK)) + name.substring(name.indexOf(POINT));
            File file = new File(newName);
            while (file.exists()) {
                try {
                    file.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                    WaitUtils.waitSomeTime(TIME_TO_WAIT);
                }
            }
            Files.write(file.toPath(), Files.readAllBytes(pathToJar.toPath()), StandardOpenOption.CREATE_NEW);
            FileUtils.launchSelf(argList.toArray(new String[1]), file.getAbsoluteFile().toPath());
        } else {
            lookupForTempFilesAndRemoveThem();
            argListConsumer.accept(argList);
        }
    }

    private static void lookupForTempFilesAndRemoveThem() {
        File pathToJar = FileUtils.getPathToJar();
        int index1 = pathToJar.getName().indexOf(POINT);
        int index2 = pathToJar.getName().indexOf(HYPHEN);
        String name = pathToJar.getName().substring(0, index1 < index2 || index2 == -1 ? index1 : index2);
        for (File file : pathToJar.getParentFile().listFiles()) {
            if (file.getName().contains(TEMP_FILE_MARK) && file.getName().contains(name)) {
                file.delete();
            }
        }
    }

    private static volatile boolean stop = false;
    private static volatile Integer port;

    public void startHttpServer(Integer port) throws IOException {
        this.port = port;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                if (stop) {
                    break;
                }
                new Thread(() -> {
                    try {
                        handleSocket(socket);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }
    }

    public static void stopHttpServer() throws IOException {
        stop = true;
        try (Socket socket = new Socket("127.0.0.1", port)) {
        }
    }

    private void handleSocket(Socket socket) throws Exception {
        try (BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE);
                BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE);) {
            readInputStream(inputStream, outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }

    private void readInputStream(BufferedInputStream inputStream, BufferedOutputStream outputStream) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int read = 0;
        do {
            byte[] buffer = new byte[BUFFER_SIZE];
            int startOfStreamContains = -1;
            int endOfStreamContains = -1;
            do {
                read = inputStream.read(buffer);
                if (read < 0 && !stop) {
                    break;
                }
                byteArrayOutputStream.write(buffer, 0, read);
                startOfStreamContains = contains(byteArrayOutputStream.toByteArray(), startOfStream.getBytes(), 0);
                if (startOfStreamContains > 0) {
                    endOfStreamContains = contains(byteArrayOutputStream.toByteArray(), endOfStream, startOfStreamContains);
                }
            } while (read > 0 && (startOfStreamContains > 0 && endOfStreamContains == -1));
            Entry<String, Integer> headEntry = getHead(byteArrayOutputStream.toByteArray());
            if (headEntry == null) {
                String request = new String(byteArrayOutputStream.toByteArray());
                Logger.log(getHttpMethod(request));
                makeStandartOutput(outputStream, request);
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
            Logger.log("command: " + command.getKey() + " value: " + command.getValue() + "!");
            commandHandlerMap.get(command.getKey()).get().handle(byteArrayOutputStream, inputStream, length, outputStream, headIndex, command.getValue());
            if (read < BUFFER_SIZE) {
                break;
            }
        } while (read > 0);
    }

    private String getHttpMethod(String request) {
        try {
            return request.substring(0, request.indexOf(endStr));
        } catch (Exception e) {
            return request;
        }
    }

    private void print(ByteArrayOutputStream byteArrayOutputStream) {
        byte[] array = byteArrayOutputStream.toByteArray();
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : array) {
            stringBuilder.append(byteToHex(b));
        }
        Logger.log(stringBuilder.toString());
    }

    public String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }

    public static final String startOfStream = "KIOKLE: KIOKLE";
    public final static byte[] endOfStream = new byte[]{0, 0, 10, 10, 10, 10, 0, 0};
    public static final String endStr = "\n";
    public static final String endStr2 = "\r";
    public static final byte[] end = endStr.getBytes();
    public static final String headEndStr = "\n\n";
    public static final String headEndStr2 = "\r\n\r\n";
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
        for (int i = startIndex; i < bytes.length; i++) {
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
