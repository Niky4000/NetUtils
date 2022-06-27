package com.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @author me
 */
public class Utils {

	private static final int BUFFER_SIZE = 1024 * 1024;

	public static File getPathToSelfJar() {
		try {
			File file = new File(Utils.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			if (file.getAbsolutePath().contains("classes")) { // Launched from debugger!
				file = Arrays.stream(file.getParentFile().listFiles()).filter(localFile -> localFile.getName().contains(".jar")).findFirst().get();
			}
			return file;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void serializeObject(File serfile, Object obj) {
		if (!serfile.getParentFile().exists()) {
			serfile.getParentFile().mkdirs();
		}
		if (serfile.exists()) {
			serfile.delete();
		}
		try (OutputStream buffer = new BufferedOutputStream(new FileOutputStream(serfile), BUFFER_SIZE); // Do not use zip!
				ObjectOutput output = new ObjectOutputStream(buffer);) {
			output.writeObject(obj);
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	public <T> T deSerializeObject(File serfile) {
		if (!serfile.exists()) {
			return null;
		}
		try (InputStream file = new FileInputStream(serfile);
				InputStream buffer = new BufferedInputStream(file, BUFFER_SIZE); // Do not use zip!
				ObjectInput input = new ObjectInputStream(buffer);) {
			T obj = (T) input.readObject();
			return obj;
		} catch (ClassNotFoundException | IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	public static boolean fireWall(int port, Socket clientSocket, Map<Integer, Set<String>> filterMap) {
		InetAddress inetAddress = clientSocket.getInetAddress();
		String hostIp = inetAddress.getHostAddress();
		return Optional.ofNullable(filterMap.get(port)).map(set -> !set.stream().filter(ip -> hostIp.contains(ip)).findAny().isPresent()).orElse(false);
	}
}
