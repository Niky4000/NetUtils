package com.servermanager.services.bean;

import com.servermanager.StartServerManager;
import com.utils.FileUtils;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.Iterator;

public class FileDownloadOutputObject<T> extends TransferObject<T> {

	private final File from;
	private final File to;
	private FileInputStream inputStream;
	private Iterator<FileUploadInputObject> iterator;
	private final String uuid;
	private final Date eventDate;

	public FileDownloadOutputObject(String uuid, Date eventDate) {
		super();
		this.from = null;
		this.to = null;
		this.uuid = uuid;
		this.eventDate = eventDate;
	}

	public FileDownloadOutputObject(File from, File to, String uuid, Date eventDate) {
		this.from = from;
		this.to = to;
		this.deadPill = false;
		this.uuid = uuid;
		this.eventDate = eventDate;
	}

	@Override
	public TransferObject apply(TransferObject<T> object, StartServerManager startServerManager) {
		if (object == null) {
			if (!from.exists()) {
				return new FileUploadInputObject(uuid, eventDate);
			}
			if (from.isDirectory()) {
				return new FileUploadInputObject(uuid, eventDate);
			}
			if (from.exists() && from.length() == 0L) {
				return new FileUploadInputObject<Object>(to, new byte[]{}, uuid, eventDate).setDeadPill(true);
			}
			try {
				inputStream = new FileInputStream(from);
				iterator = FileUtils.getFileUploadInputObjectIterator(to, inputStream, uuid, eventDate, startServerManager);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			inputStream = ((FileDownloadOutputObject) object).getInputStream();
			iterator = ((FileDownloadOutputObject) object).getIterator();
		}
		if (iterator.hasNext()) {
			FileUploadInputObject next = iterator.next();
			return next;
		} else {
			finish();
			return new FileUploadInputObject(uuid, eventDate);
		}
	}

	@Override
	public void finish() {
		if (inputStream != null) {
			try {
				inputStream.close();
				Files.setLastModifiedTime(to.toPath(), FileTime.fromMillis(eventDate.getTime()));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		inputStream = null;
	}

	public File getFile() {
		return from;
	}

	public FileInputStream getInputStream() {
		return inputStream;
	}

	public Iterator<FileUploadInputObject> getIterator() {
		return iterator;
	}
}
