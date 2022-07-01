package com.servermanager.services.bean;

import com.utils.FileUtils;
import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.Iterator;

public class FileDownloadOutputObject<T> extends TransferObject<T> {

	private final File from;
	private final File to;
	private FileInputStream inputStream;
	private Iterator<FileUploadInputObject> iterator;
	private final Date eventDate;

	public FileDownloadOutputObject(Date eventDate) {
		super();
		this.from = null;
		this.to = null;
		this.eventDate = eventDate;
	}

	public FileDownloadOutputObject(File from, File to, Date eventDate) {
		this.from = from;
		this.to = to;
		this.deadPill = false;
		this.eventDate = eventDate;
	}

	@Override
	public TransferObject apply(TransferObject<T> object) {
		if (object == null) {
			if (from.isDirectory()) {
				return new FileUploadInputObject(eventDate);
			}
			if (from.exists() && from.length() == 0L) {
				return new FileUploadInputObject<Object>(to, new byte[]{}, eventDate).setDeadPill(true);
			}
			try {
				inputStream = new FileInputStream(from);
				iterator = FileUtils.getFileUploadInputObjectIterator(to, inputStream, eventDate);
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
			return new FileUploadInputObject(eventDate);
		}
	}

	@Override
	public void finish() {
		if (inputStream != null) {
			try {
				inputStream.close();
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
