package ru.kiokle.simplehttpserver;

import java.io.File;
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
}
