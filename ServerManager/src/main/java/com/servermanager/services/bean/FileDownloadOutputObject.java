package com.servermanager.services.bean;

import com.utils.FileUtils;
import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;

public class FileDownloadOutputObject<T> extends TransferObject<T> {

	private final File from;
	private final File to;
	private FileInputStream inputStream;
	private Iterator<FileUploadInputObject> iterator;

	public FileDownloadOutputObject() {
		super();
		this.from = null;
		this.to = null;
	}

	public FileDownloadOutputObject(File from, File to) {
		this.from = from;
		this.to = to;
		this.deadPill = false;
	}

	@Override
	public TransferObject apply(TransferObject<T> object) {
		if (object == null) {
			try {
				inputStream = new FileInputStream(from);
				iterator = FileUtils.getFileUploadInputObjectIterator(to, inputStream);
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
			return new FileUploadInputObject();
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
