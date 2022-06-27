package com.servermanager.services.bean;

import java.io.Serializable;

public class TransferObject<T> implements Serializable {

	protected boolean deadPill = false;

	public TransferObject() {
		deadPill = true;
	}

	public boolean isDeadPill() {
		return deadPill;
	}

	public TransferObject setDeadPill(boolean deadPill) {
		this.deadPill = deadPill;
		return this;
	}

	public TransferObject apply(TransferObject<T> object) {
		return new TransferObject();
	}

	public void finish() {
	}
}
