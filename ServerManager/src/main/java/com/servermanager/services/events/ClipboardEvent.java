package com.servermanager.services.events;

import java.util.Objects;

public class ClipboardEvent extends Event {

	private final String clipboardData;

	public ClipboardEvent(String clipboardData) {
		this.clipboardData = clipboardData;
	}

	public String getClipboardData() {
		return clipboardData;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 97 * hash + Objects.hashCode(this.clipboardData);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ClipboardEvent other = (ClipboardEvent) obj;
		if (!Objects.equals(this.clipboardData, other.clipboardData)) {
			return false;
		}
		return true;
	}
}
