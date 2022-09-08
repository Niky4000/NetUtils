package ru.kiokle.simplehttpserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StartSimpleHttpServer {

	private static final int BUFFER_SIZE = 1024 * 1024;

	public static void main(String[] args) throws IOException {
		List<String> argList = Stream.of(args).collect(Collectors.toList());
		Integer port = Integer.valueOf(getConfig("-port", argList));
		startHttpServer(port);
	}

	public static void startHttpServer(Integer port) throws IOException {
		ServerSocket serverSocket = new ServerSocket(port);
		while (true) {
			try {
				Socket socket = serverSocket.accept();
				handleSocket(socket);
			} catch (Exception e) {
				String ex = e.getMessage();
			}
		}
	}

	private static void handleSocket(Socket socket) throws IOException {
		try (InputStream inputStream = new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE);
				OutputStream outputStream = new BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE);) {
			byte[] readInputStream = readInputStream(inputStream);
			String string = new String(readInputStream);
			byte[] data = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " Hello World!!!").getBytes();
			String headers = "HTTP/1.1 200\n"
					+ "content-length: " + data.length + "\n"
					+ "cache-control: no-cache\n"
					+ "content-type: text/html\n"
					+ "connection: close\n\n";
			outputStream.write(headers.getBytes());
			outputStream.write(data);
			outputStream.flush();
		} finally {
			socket.close();
		}
	}

	private static byte[] readInputStream(InputStream inputStream) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		int read = 0;
		do {
			byte[] buffer = new byte[BUFFER_SIZE];
			read = inputStream.read(buffer);
			byteArrayOutputStream.write(buffer, 0, read);
			if (read < BUFFER_SIZE) {
				break;
			}
		} while (read > 0);
		return byteArrayOutputStream.toByteArray();
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
