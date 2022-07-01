package com.servermanager.services.bean;

import com.servermanager.StartServerManager;

import static com.utils.FileUtils.handleFile;

import java.io.File;
import java.util.Date;

public class FileUploadInputObject<T> extends TransferObject<T> {

	private final File file;
	private final byte[] bytes;
	private final boolean delete;
	private final Date eventDate;

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
	public TransferObject apply(TransferObject<T> object) {
		handleFile(file, bytes, delete, deadPill, file_ -> {
			if (object != null) {
				try {
					StartServerManager.getClusterService().fileUploadedEvent(((FileUploadInputObject) object).getFile(), ((FileUploadInputObject) object).getEventDate());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		return new TransferObject();
	}
}
