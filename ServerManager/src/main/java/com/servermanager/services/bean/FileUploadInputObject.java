package com.servermanager.services.bean;

import com.servermanager.StartServerManager;
import static com.utils.FileUtils.handleFile;
import static com.utils.Logger.println;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Date;

public class FileUploadInputObject<T> extends TransferObject<T> {

	protected final File file;
	protected final byte[] bytes;
	protected final boolean delete;
	protected final Date eventDate;

	public FileUploadInputObject(Date eventDate) {
		super();
		this.file = null;
		this.bytes = null;
		this.delete = false;
		this.eventDate = eventDate;
	}

	public FileUploadInputObject(File file, Date eventDate) {
		this.file = file;
		this.bytes = null;
		this.delete = true;
		this.deadPill = false;
		this.eventDate = eventDate;
	}

	public FileUploadInputObject(File file, byte[] bytes, Date eventDate) {
		this.file = file;
		this.bytes = bytes;
		this.delete = false;
		this.deadPill = false;
		this.eventDate = eventDate;
	}

	public File getFile() {
		return file;
	}

	public Date getEventDate() {
		return eventDate;
	}

	@Override
	public TransferObject apply(TransferObject<T> object, StartServerManager startServerManager) {
		handleFile(file, bytes, delete, deadPill, file_ -> {
			if (object != null) {
				try {
					startServerManager.getClusterService().fileUploadedEvent(((FileUploadInputObject) object).getFile(), ((FileUploadInputObject) object).getEventDate());
				} catch (Exception e) {
					println(e);
				}
			}
		});
		if (file != null && file.exists()) {
			try {
				Files.setLastModifiedTime(file.toPath(), FileTime.fromMillis(eventDate.getTime()));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return new TransferObject();
	}
}
