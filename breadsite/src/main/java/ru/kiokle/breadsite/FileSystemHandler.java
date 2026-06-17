package ru.kiokle.breadsite;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static ru.kiokle.breadsite.BreadSiteStart.endStr;

public class FileSystemHandler {

    private static final String pathVariableName = "path=";
    private static final String backVariableName = "back=";
    private static final String COMMENT_START = "<!--";
    private static final String COMMENT_END = "-->";
    private static final String NEW_LINE = "\n";
    private static final String NEW_LINE0 = "\r";
    private final FileUtils fileUtils;
    private final String basePath;
    private final Map<String, String> handleMap;

    public FileSystemHandler() throws IOException {
        this.fileUtils = new FileUtils();
        basePath = new File((String) fileUtils.getConfigs().get("base")).getAbsolutePath();
        handleMap = fileUtils.getEnvConfigs().entrySet().stream().collect(Collectors.toMap(o -> (String) o.getKey(), o -> (String) o.getValue()));
    }

    public HttpResponse getData(String request) throws IOException {
        File baseDir = getPathFromRequest(request, "GET ");
        String type = getType(baseDir);
        byte[] content = handle(FileUtils.readAllBytesFromFile(baseDir), type);
        return new HttpResponse(content, ("HTTP/1.1 200\n"
                + "content-length: " + content.length + "\n"
                + "cache-control: no-cache\n"
                + "content-type: " + type + "\n"
                + "connection: close\n\n").getBytes());
    }

    private boolean isParentDir(String request) {
        return request.substring(0, request.indexOf(endStr)).contains(backVariableName);
    }

    private File getPathFromRequest(String request, String fieldName) {
        if (request.contains(fieldName)) {
            int startIndex = request.indexOf(fieldName) + fieldName.length();
            int endIndex = request.indexOf(" ", startIndex);
            int endIndex2 = request.indexOf("&", startIndex);
            int endIndex3 = request.indexOf("\n", startIndex);
            String dirStr = handle(request.substring(startIndex, min(endIndex, endIndex2, endIndex3)).replace("%2F", "/").replace("%3A", ":").replace("%5C", "\\"));
            File dir = new File(dirStr);
            if (dir.exists()) {
                if (isParentDir(request)) {
                    File parentDir = dir.getParentFile();
                    if (parentDir.exists()) {
                        return parentDir;
                    } else {
                        return dir;
                    }
                } else {
                    return dir;
                }
            } else {
                String baseDir = FileUtils.getPathToJar().getParent();
                return new File(baseDir);
            }
        } else {
            String baseDir = FileUtils.getPathToJar().getParent();
            return new File(baseDir);
        }
    }

    private String handle(String str) {
        System.out.println("path = " + str);
        return new File(basePath + str).isDirectory() ? basePath + str + "index.html" : basePath + str;
    }

    private int min(int... i) {
        return IntStream.of(i).filter(j -> j >= 0).min().getAsInt();
    }

    private String getType(File file) {
        try {
            String name = file.getName() + "\n";
            int index = name.contains("?") ? getClosestIndex(name) : name.lastIndexOf(".");
            int index2 = name.indexOf("?", index);
            int index3 = name.lastIndexOf("\n");
            String substring = name.substring(index + 1, min(index2, index3));
            if (substring.equals("css")) {
                return "text/css";
            } else if (substring.equals("js")) {
                return "text/javascript";
            } else if (substring.equals("png")) {
                return "image/png";
            } else if (substring.equals("jpeg")) {
                return "image/jpeg";
            } else if (substring.equals("jpg")) {
                return "image/jpeg";
            } else if (substring.equals("svg")) {
                return "image/svg+xml";
            } else if (substring.equals("ico")) {
                return "image/x-icon";
            } else {
                return "text/html";
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private int getClosestIndex(String str) {
        int i = 1;
        int index2 = str.indexOf("?");
        int startingIndex = 0;
        while (i > 0 && i < index2) {
            i = str.indexOf(".", startingIndex + 1);
            if (i > 0 && i < index2) {
                startingIndex = i;
            }
        };
        return startingIndex;
    }

    private byte[] handle(byte[] content, String type) {
        if ("text/html".equals(type)) {
            String string = new String(content);
            for (Entry<String, String> env : handleMap.entrySet()) {
                string = string.replace(env.getKey(), env.getValue());
            }
            return removeComments(string).getBytes();
        } else {
            return content;
        }
    }

    private String removeComments(String str) {
        if (str.contains(COMMENT_START)) {
            StringBuilder sb = new StringBuilder();
            int startIndex = 0;
            int previousIndex2 = 0;
            while (startIndex >= 0) {
                int indexOf = str.indexOf(COMMENT_START, startIndex);
                int indexOf2 = str.indexOf(COMMENT_END, startIndex + COMMENT_START.length());
                if (indexOf >= 0 && indexOf2 >= 0) {
                    String substring = str.substring(startIndex, indexOf);
                    sb.append(substring);
                    startIndex = indexOf2 + COMMENT_END.length();
                    previousIndex2 = indexOf2 + COMMENT_END.length();
                } else {
                    startIndex = -1;
                    if (indexOf2 + COMMENT_END.length() < str.length()) {
                        sb.append(str.substring(previousIndex2, str.length()));
                    }
                }
            }
            return sb.toString();
        } else {
            return str;
        }
    }

    private String removeNewLines(String str) {
        if (str.contains(NEW_LINE)) {
            StringBuilder sb = new StringBuilder();
            int startIndex = 0;
            while (startIndex >= 0) {
                boolean twoSymbols = false;
                int indexOf = str.indexOf(NEW_LINE, startIndex);
                if (indexOf >= 0) {
                    if (indexOf - 1 >= 0 && str.substring(indexOf - 1, indexOf).equals(NEW_LINE0)) {
                        indexOf--;
                        twoSymbols = true;
                    }
                    int i = 0;
                    for (i = indexOf; i < str.length(); i++) {
                        String substring = str.substring(i, i + 1);
                        if (!(substring.equals(NEW_LINE0) || substring.equals(NEW_LINE))) {
                            break;
                        }
                    }
                    String substring = str.substring(startIndex, indexOf);
                    sb.append(substring);
                    sb.append(twoSymbols ? NEW_LINE0 + NEW_LINE : NEW_LINE);
                    startIndex = indexOf + NEW_LINE.length();
                } else {
                    sb.append(str.substring(startIndex, str.length()));
                    break;
                }
            }
            return sb.toString();
        } else {
            return str;
        }
    }
}
