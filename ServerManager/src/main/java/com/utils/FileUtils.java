package com.utils;

import com.servermanager.StartServerManager;
import static com.servermanager.observable.threads.FileSystemObserverThread.nullDate;
import com.servermanager.services.UpdateSshService;
import com.servermanager.services.bean.FileUploadInputObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class FileUtils {

	public static final int buffer_size = 1024 * 1024;

	public static void handleFile(File file, byte[] bytes, boolean delete, boolean deadPill, Consumer<File> fileUploaded) throws RuntimeException {
		try {
			if (!deadPill && file != null && !file.isDirectory()) {
				OpenOption openOption;
				if (file.exists()) {
					if (delete) {
						openOption = CREATE_NEW;
						file.delete();
					} else {
						openOption = TRUNCATE_EXISTING;
					}
				} else {
					openOption = CREATE_NEW;
				}
				if (file.getParentFile() != null && !file.getParentFile().exists()) {
					file.getParentFile().mkdirs();
				}
				if (bytes != null) {
					Files.write(file.toPath(), bytes, openOption);
				} else if (bytes == null && openOption.equals(CREATE_NEW)) {
					file.createNewFile();
				}
			} else if (deadPill && bytes != null && bytes.length == 0 && file != null && !file.exists()) {
				file.createNewFile();
				fileUploaded.accept(file);
			} else {
				fileUploaded.accept(file);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Iterator<FileUploadInputObject> getFileUploadInputObjectIterator(File file, FileInputStream inputStream, Date eventDate, StartServerManager startServerManager) {
		AtomicBoolean finished = new AtomicBoolean(false);
		Iterator<FileUploadInputObject> iterator = new Iterator<FileUploadInputObject>() {
			@Override
			public boolean hasNext() {
				return !finished.get();
			}

			@Override
			public FileUploadInputObject next() {
				try {
					byte[] buffer = new byte[buffer_size];
					int read = inputStream.read(buffer);
					if (read < 0) {
						finished.set(true);
					} else {
						if (read < buffer_size) {
							byte[] buffer2 = new byte[read];
							System.arraycopy(buffer, 0, buffer2, 0, read);
							return new FileUploadInputObject<Object>(file, buffer2, eventDate);
						} else {
							return new FileUploadInputObject<Object>(file, buffer, eventDate);
						}
					}
					return new FileUploadInputObject<>(eventDate);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};
		return iterator;
	}

	public static File getPathToJar() {
		try {
			File file = new File(UpdateSshService.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			if (file.getAbsolutePath().contains("classes")) { // Launched from debugger!
				file = Arrays.stream(file.getParentFile().listFiles()).filter(localFile -> localFile.getName().endsWith(".jar")).findFirst().get();
			}
			return file;
		} catch (Exception e) {
			throw new RuntimeException(e);
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
		List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
		String commandLineOptions = inputArguments.stream().reduce("", (str1, str2) -> str1 + " " + str2);
		String commandLineArguments = Stream.of(args).reduce("", (str1, str2) -> str1 + " " + str2);
		String exec = "java" + commandLineOptions + " -jar " + to.toFile().getAbsolutePath() + commandLineArguments;
		Process process = Runtime.getRuntime().exec(exec);
		return process;
	}

	public static AtomicBoolean getFileCondition(StartServerManager startServerManager1, File file, AtomicReference<Date> lastModifiedTime) {
		AtomicBoolean conditon = new AtomicBoolean(false);
		startServerManager1.getDownloadedFiles().asMap().computeIfPresent(file.getAbsolutePath(), (path, date) -> {
			if (lastModifiedTime.get() != null && !lastModifiedTime.get().after(date)) {
				conditon.set(true);
			}
			return date;
		});
		return conditon;
	}

	public static AtomicReference<Date> getLastModifiedTime(File file) throws IOException {
		AtomicReference<Date> lastModifiedTime = new AtomicReference<>();
		if (file.exists()) {
			BasicFileAttributes attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
			lastModifiedTime.set(new Date(attributes.lastModifiedTime().toMillis()));
		}
		return lastModifiedTime;
	}
}
