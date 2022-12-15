package com.servermanager.services.bean;

import com.servermanager.StartServerManager;
import static com.utils.Logger.println;
import java.io.File;
import java.util.Date;
import java.util.Optional;

public class FileDeleteInputObject<T> extends TransferObject<T> {

	private final File file;
	private final Date eventDate;

	public FileDeleteInputObject(File file, Date eventDate) {
		this.file = file;
		this.eventDate = eventDate;
	}

	@Override
	public TransferObject apply(TransferObject<T> object, StartServerManager startServerManager) {
		file.delete();
		try {
			startServerManager.getClusterService().fileDeletedEvent(file, eventDate);
		} catch (Exception e) {
			println(e);
		}
		if (file.getParentFile().listFiles().length == 0 && Optional.ofNullable(startServerManager.getClusterService()).map(cluster -> !cluster.getHome().equals(file.getParentFile())).orElse(true)) {
			file.getParentFile().delete();
		}
		return new TransferObject();
	}
}
