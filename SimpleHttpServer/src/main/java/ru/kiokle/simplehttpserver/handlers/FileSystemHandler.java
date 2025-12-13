package ru.kiokle.simplehttpserver.handlers;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.endStr;
import static ru.kiokle.simplehttpserver.StartSimpleHttpServer.endStr2;
import ru.kiokle.simplehttpserver.utils.FileUtils;

public class FileSystemHandler {

    private static final String pathVariableName = "path=";
    private static final String backVariableName = "back=";

    public String getData(String request) throws IOException {
        File baseDir = getPathFromRequest(request, pathVariableName);
        String content = new String(FileUtils.readAllBytesFromResource(CommandHandler.class, "index.html"));
        content = content.replace("$dateTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " Hello World!!!");
        content = content.replace("$pathValue", baseDir.getAbsolutePath());
        content = content.replace("$fileSystemTable", getFileSystemTable(baseDir));
        return content;
    }

    private boolean isParentDir(String request) {
        return request.substring(0, request.indexOf(endStr)).contains(backVariableName);
    }

    private String getFileSystemTable(File baseDir) {
        StringBuilder stringBuilder = new StringBuilder("<table width=\"100%\">").append(endStr2).append(endStr);
        for (File file : baseDir.listFiles()) {
            if (file.isDirectory()) {
                stringBuilder.append("<tr><td><a href=\"/?");
                stringBuilder.append(pathVariableName);
                stringBuilder.append(file.getAbsolutePath().replace("/", "%2F").replace(":", "%3A").replace("\\", "%5C"));
                stringBuilder.append("\">");
                stringBuilder.append(file.getAbsolutePath());
                stringBuilder.append("</td><td>Dir</td></tr>").append(endStr2).append(endStr);
            } else {
                stringBuilder.append("<tr><td>");
                stringBuilder.append(file.getAbsolutePath());
                stringBuilder.append("</td><td>");
                stringBuilder.append(file.length());
                stringBuilder.append("</td></tr>").append(endStr2).append(endStr);
            }
        }
        stringBuilder.append("</table>");
        return stringBuilder.toString();
    }

    private File getPathFromRequest(String request, String fieldName) {
        if (request.contains(fieldName)) {
            int startIndex = request.indexOf(fieldName) + fieldName.length();
            int endIndex = request.indexOf(" ", startIndex + fieldName.length());
            int endIndex2 = request.indexOf("&", startIndex + fieldName.length());
            String dirStr = request.substring(startIndex, min(endIndex, endIndex2)).replace("%2F", "/").replace("%3A", ":").replace("%5C", "\\");
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

    private int min(int i, int j) {
        return i < j || j == -1 ? i : j;
    }
}
