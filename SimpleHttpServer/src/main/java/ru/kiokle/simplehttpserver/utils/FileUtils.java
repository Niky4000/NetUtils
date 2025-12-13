package ru.kiokle.simplehttpserver.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import ru.kiokle.simplehttpserver.log.Logger;

public class FileUtils {

    private static final String JAR = "jar:file:";
    private static final String FILE = "file:";
    private static final String EXCLAMATION = "!";

    public static File getPathToJar() {
        try {
            File file = new File(handleUri(FileUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI().toString()));
            if (file.getAbsolutePath().contains("classes")) { // Launched from debugger!
                file = Arrays.stream(file.getParentFile().listFiles()).filter(localFile -> localFile.getName().endsWith(".jar") && !localFile.getName().contains("-")).findFirst().get();
            }
            return file;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String handleUri(String uri) {
        return removeBefore(removeBefore(removeAfter(uri, EXCLAMATION), JAR), FILE);
    }

    private static String removeBefore(String str, String whatToRemove) {
        if (str.contains(whatToRemove)) {
            return str.substring(str.indexOf(whatToRemove) + whatToRemove.length());
        } else {
            return str;
        }
    }

    private static String removeAfter(String str, String whatToRemove) {
        if (str.contains(whatToRemove)) {
            return str.substring(0, str.indexOf(whatToRemove));
        } else {
            return str;
        }
    }
    private static final int DIGEST_BUFFER = 1024 * 1024 * 10;
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String getMd5Sum(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            //Create byte array to read data in chunks
            try ( //Get file input stream for reading the file content
                    FileInputStream fis = new FileInputStream(file)) {
                //Create byte array to read data in chunks
                byte[] byteArray = new byte[DIGEST_BUFFER];
                int bytesCount = 0;
                //Read file data and update in message digest
                while ((bytesCount = fis.read(byteArray)) != -1) {
                    md.update(byteArray, 0, bytesCount);
                }
            }
            //Get the hash's bytes
            byte[] digest = md.digest();
            String bytesToHex = bytesToHex(digest);
            return bytesToHex;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static Process launchSelf(String[] args, Path to) throws IOException {
        String exec = getLaunchArguments(args, to);
        Logger.log(exec);
        Process process = Runtime.getRuntime().exec(exec);
        return process;
    }

    public static String getLaunchArguments(String[] args, Path to) {
        List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        String commandLineOptions = inputArguments.stream().reduce("", (str1, str2) -> str1 + " " + str2);
        String commandLineArguments = Stream.of(args).reduce("", (str1, str2) -> str1 + " " + str2);
        String exec = incrementAddress("java" + commandLineOptions + " -jar " + to.toFile().getAbsolutePath() + commandLineArguments);
        return exec;
    }

    private static final String address = "address=";

    public static String incrementAddress(String commandLineArgument) {
        if (commandLineArgument.contains(address)) {
            int addressIndex = commandLineArgument.indexOf(address);
            int endIndex = commandLineArgument.indexOf(" ", addressIndex + address.length());
            String portStr = commandLineArgument.substring(addressIndex + address.length(), endIndex);
            Integer debugPort = Integer.valueOf(portStr);
            debugPort++;
            String newCommandLineArgument = commandLineArgument.replace(portStr, debugPort.toString());
            return newCommandLineArgument;
        } else {
            return commandLineArgument;
        }
    }

    public static final int LOCAL_BUFFER_SIZE = 1024 * 1024;

    public static byte[] readAllBytesFromResource(Class cl, String resourcePath) throws IOException {
        try (BufferedInputStream resourceAsStream = new BufferedInputStream(cl.getClassLoader().getResourceAsStream(resourcePath), LOCAL_BUFFER_SIZE)) {
            byte[] buffer = new byte[resourceAsStream.available()];
            resourceAsStream.read(buffer);
            return buffer;
        }
    }
}
