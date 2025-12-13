package com.servermanager.services.events;

import java.io.Serializable;
import java.util.Objects;

public class FileEventKey implements Serializable {

	private final String name;

	public FileEventKey(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 97 * hash + Objects.hashCode(this.name);
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
		final FileEventKey other = (FileEventKey) obj;
		if (!Objects.equals(this.name, other.name)) {
			return false;
		}
		return true;
	}
}
