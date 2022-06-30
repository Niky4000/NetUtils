package com.servermanager.caches;

public enum CacheNames {
	EVENTS("events"), FILES("files");

	private final String value;

	CacheNames(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}
}
