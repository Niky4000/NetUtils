package ru.kiokle.simplehttpserver.utils;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.Arrays;

public class FileUtils {

    private static final String JAR = "jar:file:";
    private static final String FILE = "file:";
    private static final String EXCLAMATION = "!";

    public static File getPathToJar() {
        try {
            File file = new File(handleUri(FileUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI().toString()));
            if (file.getAbsolutePath().contains("classes")) { // Launched from debugger!
                file = Arrays.stream(file.getParentFile().listFiles()).filter(localFile -> localFile.getName().endsWith(".jar")).findFirst().get();
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
}
