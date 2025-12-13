package com.servermanager.services;

import static com.servermanager.services.UpdateService.UPDATE;
import com.utils.FileUtils;
import static com.utils.FileUtils.launchSelf;
import com.utils.WaitUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.stream.Stream;

public class SelfUpdateService {

	private static final int TIME_TO_WAIT = 10;

	public void update(String[] args) throws IOException {
		File pathToJar = FileUtils.getPathToJar();
		if (pathToJar.getName().contains(UPDATE)) {
			WaitUtils.waitSomeTime(TIME_TO_WAIT);
			String absolutePath = pathToJar.getAbsolutePath();
			String absolutePathOfTheJar = absolutePath.substring(0, absolutePath.indexOf(UPDATE)) + absolutePath.substring(absolutePath.indexOf(UPDATE) + UPDATE.length());
			File file = new File(absolutePathOfTheJar);
			boolean deleted = false;
			do {
				deleted = file.delete();
				if (!deleted) {
					WaitUtils.waitSomeTime(TIME_TO_WAIT);
				}
			} while (!deleted);
			Files.write(file.toPath(), Files.readAllBytes(pathToJar.toPath()), StandardOpenOption.CREATE_NEW);
			launchSelf(args, file.toPath());
			System.exit(0);
		} else {
			Optional<File> updateFile = Stream.of(pathToJar.getParentFile().listFiles()).filter(file -> file.getName().contains(UPDATE)).findFirst();
			updateFile.ifPresent(file -> file.delete());
		}
	}
}
