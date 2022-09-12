package ru.kiokle.jftp;

import com.guichaguri.minimalftp.FTPServer;
import com.guichaguri.minimalftp.impl.NativeFileSystem;
import com.guichaguri.minimalftp.impl.NoOpAuthenticator;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class StartFtp {

	public static void main(String[] args) throws IOException {
		List<String> argList = Arrays.asList(args);
		String dir = getConfig("-dir", argList);
		Integer port = Integer.valueOf(getConfig("-port", argList));
		// Uses the current working directory as the root
		File root = new File(dir);
		// Creates a native file system
		NativeFileSystem fs = new NativeFileSystem(root);
		// Creates a noop authenticator, which allows anonymous authentication
		NoOpAuthenticator auth = new NoOpAuthenticator(fs);
		// Creates the server with the authenticator
		FTPServer server = new FTPServer(auth);
		// Start listening synchronously
		server.listenSync(port);
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
