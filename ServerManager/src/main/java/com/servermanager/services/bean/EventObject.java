package com.servermanager.services.bean;

import com.servermanager.StartServerManager;

public class EventObject<T> extends TransferObject<T> {

	@Override
	public TransferObject apply(TransferObject<T> object, StartServerManager startServerManager) {
		return new EventObject();
	}
}
