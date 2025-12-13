package com.servermanager.services.bean;

import com.servermanager.StartServerManager;
import com.servermanager.services.events.ClipboardEvent;

public class ClipboardObject extends TransferObject<String> {

	private final ClipboardEvent clipboardEvent;

	public ClipboardObject(ClipboardEvent clipboardEvent) {
		this.clipboardEvent = clipboardEvent;
	}

	public ClipboardEvent getClipboardEvent() {
		return clipboardEvent;
	}

	@Override
	public TransferObject apply(TransferObject<String> object, StartServerManager startServerManager) {
		startServerManager.getClusterService().clipboardChanged(clipboardEvent);
		return new TransferObject();
	}
}
