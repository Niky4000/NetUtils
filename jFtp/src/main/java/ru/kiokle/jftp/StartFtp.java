package ru.kiokle.jftp;

import com.guichaguri.minimalftp.FTPServer;
import com.guichaguri.minimalftp.impl.NativeFileSystem;
import com.guichaguri.minimalftp.impl.NoOpAuthenticator;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.IntStream;

public class StartFtp {

	public static void main(String[] args) throws IOException {
		List<String> argList = Arrays.asList(args);
		if (!argList.contains("-dir") || !argList.contains("-port") || !argList.contains("-pr")) {
			System.out.println("Usage:\n"
					+ "-dir - base directory\n"
					+ "-port - base directory\n"
					+ "-pr - ftp port range. Example: 30000-30020");
		} else {
			String dir = getConfig("-dir", argList);
			Integer port = Integer.valueOf(getConfig("-port", argList));
			String[] split = getConfig("-pr", argList).split("-");
			Integer lowerBound = Integer.valueOf(split[0]);
			Integer upperBound = Integer.valueOf(split[1]);
			// Uses the current working directory as the root
			File root = new File(dir);
			// Creates a native file system
			NativeFileSystem fs = new NativeFileSystem(root);
			// Creates a noop authenticator, which allows anonymous authentication
			NoOpAuthenticator auth = new NoOpAuthenticator(fs);
			// Creates the server with the authenticator
			FTPServer server = new FTPServer(auth, lowerBound, upperBound);
			// Start listening synchronously
			server.listenSync(port);
		}
	}

	public static String getConfig(String arg, List<String> argList) {
		int indexOf = argList.indexOf(arg);
		if (indexOf >= 0) {
			return argList.get(indexOf + 1);
		} else {
			return null;
		}
	}

	public static List<String> getConfigList(String arg, List<String> argList) {
		int indexOf = argList.indexOf(arg);
		if (indexOf >= 0) {
			List<String> list = new ArrayList<>();
			for (int i = indexOf + 1; i < argList.size(); i++) {
				list.add(argList.get(i));
			}
			return list;
		} else {
			return null;
		}
	}
}
