package com.servermanager.services.bean;

import com.servermanager.StartServerManager;
import static com.utils.FileUtils.handleFile;
import java.io.File;

public class FileUploadInputObject<T> extends TransferObject<T> {

	private final File file;
	private final byte[] bytes;
	private final boolean delete;

	public FileUploadInputObject() {
		super();
		this.file = null;
		this.bytes = null;
		this.delete = false;
	}

	public FileUploadInputObject(File file) {
		this.file = file;
		this.bytes = null;
		this.delete = true;
		this.deadPill = false;
	}

	public FileUploadInputObject(File file, byte[] bytes) {
		this.file = file;
		this.bytes = bytes;
		this.delete = false;
		this.deadPill = false;
	}

	public File getFile() {
		return file;
	}

	@Override
	public TransferObject apply(TransferObject<T> object) {
		handleFile(file, bytes, delete, deadPill, file_ -> {
			if (object != null) {
				try {
					StartServerManager.getClusterService().fileUploadedEvent(((FileUploadInputObject) object).getFile());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		return new TransferObject();
	}
}
