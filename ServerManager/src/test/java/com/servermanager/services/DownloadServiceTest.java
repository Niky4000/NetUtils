package com.servermanager.services;

import com.servermanager.StartServerManager;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.stream.IntStream;
import org.junit.Assert;
import org.junit.Test;

public class DownloadServiceTest {

	private static final String HOST = "127.0.0.1";
	private static final int PORT = 2222;
	private Long fileLength = 1024L;

	@Test
	public void test() throws Exception {
		StartServerManager startServerManager = new StartServerManager();
		File file = new File("Some");
		File file2 = new File("Some2");
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			if (file2.exists()) {
				file2.delete();
			}
			Files.write(file.toPath(), IntStream.range(0, fileLength.intValue()).mapToObj(d -> "0").reduce("", (str1, str2) -> str1 + str2).getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
			final ServerListerner serverListerner = new ServerListerner(PORT, startServerManager);
			Thread thread = new Thread(() -> {
				try {
					serverListerner.listen();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			thread.start();
			DownloadService downloadService = new DownloadService(HOST, PORT, startServerManager);
			downloadService.download(file.toPath(), file2.toPath(), new Date());
			serverListerner.shutdown();
			Assert.assertTrue(file2.exists() && fileLength.equals(file2.length()));
		} finally {
			file.delete();
			file2.delete();
		}
	}
}
