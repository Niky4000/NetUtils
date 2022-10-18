package com.httptunneling.utils;

import com.httptunneling.TunnelStart;
import static com.httptunneling.TunnelStart.isIpAddressValid;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NetUtils {

	private static final int BUFFER_SIZE = 1024;

	public static ByteArrayOutputStream readInputStream(Supplier<Socket> socketSupplier) {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try (Socket socket = socketSupplier.get()) {
			try (InputStream inputStream = new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE)) {
				readImpl(inputStream, byteArrayOutputStream);
			}
		} catch (IOException ex) {
			Logger.getLogger(TunnelStart.class.getName()).log(Level.SEVERE, null, ex);
		}
		return byteArrayOutputStream;
	}

	public static void writeOutputStream(Supplier<Socket> socketSupplier, byte[] dataToWrite) {
		try (Socket socket = socketSupplier.get()) {
			try (OutputStream outputStream = new BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE)) {
				outputStream.write(dataToWrite);
				outputStream.flush();
			}
		} catch (IOException ex) {
			Logger.getLogger(TunnelStart.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private static void readImpl(final InputStream inputStream, ByteArrayOutputStream byteArrayOutputStream) throws IOException {
		int read = 0;
		do {
			byte[] buffer = new byte[BUFFER_SIZE];
			read = inputStream.read(buffer);
			byteArrayOutputStream.write(buffer, 0, read);
			if (read < BUFFER_SIZE) {
				break;
			}
		} while (read > 0);
	}

	public static Map<Integer, Set<String>> parceFilters(List<String> argList) {
		Map<Integer, Set<String>> map = new ConcurrentHashMap<>();
		List<Integer> argsToRemove = new ArrayList<>();
		for (int j = 0; j < argList.size(); j++) {
			if (argList.get(j).equals("F")) {
				argsToRemove.add(j);
				Set<String> ipSet = new CopyOnWriteArraySet<>();
				for (int i = j + 1; i < argList.size(); i++) {
					if (isIpAddressValid(argList.get(i))) {
						argsToRemove.add(i);
						ipSet.add(argList.get(i).replace("*", ""));
					} else {
						break;
					}
				}
				map.put(Integer.valueOf(argList.get(j - 1)), ipSet);
			}
		}
		for (int i = argsToRemove.size() - 1; i >= 0; i--) {
			argList.remove(argsToRemove.get(i).intValue());
		}
		return map;
	}
}
