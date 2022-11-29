package com.servermanager.services.bean;

import com.servermanager.StartServerManager;
import static com.utils.FileUtils.handleFile;
import static com.utils.Logger.println;
import java.io.File;
import java.util.Date;

public class FileUploadInputObject<T> extends TransferObject<T> {

	protected final File file;
	protected final byte[] bytes;
	protected final boolean delete;
	protected final String uuid;
	protected final Date eventDate;

	public FileUploadInputObject(String uuid, Date eventDate) {
		super();
		this.file = null;
		this.bytes = null;
		this.delete = false;
		this.uuid = uuid;
		this.eventDate = eventDate;
	}

	public FileUploadInputObject(File file, String uuid, Date eventDate) {
		this.file = file;
		this.bytes = null;
		this.delete = true;
		this.deadPill = false;
		this.uuid = uuid;
		this.eventDate = eventDate;
	}

	public FileUploadInputObject(File file, byte[] bytes, String uuid, Date eventDate) {
		this.file = file;
		this.bytes = bytes;
		this.delete = false;
		this.deadPill = false;
		this.uuid = uuid;
		this.eventDate = eventDate;
	}

	public File getFile() {
		return file;
	}

	public String getUuid() {
		return uuid;
	}

	public Date getEventDate() {
		return eventDate;
	}

	@Override
	public TransferObject apply(TransferObject<T> object, StartServerManager startServerManager) {
		handleFile(file, bytes, delete, deadPill, file_ -> {
			if (object != null) {
				try {
					startServerManager.getClusterService().fileUploadedEvent(((FileUploadInputObject) object).getFile(), ((FileUploadInputObject) object).getUuid(), ((FileUploadInputObject) object).getEventDate());
				} catch (Exception e) {
					println(e);
				}
			}
		});
		return new TransferObject();
	}
}
