package com.servermanager.test.utils;

import java.util.function.Consumer;

public class Utils {

	public static Thread th(Consumer<Boolean> runnable, String name) {
		Thread thread = new Thread(() -> {
			runnable.accept(true);
			waitForewer();
		});
		thread.setName(name);
		thread.start();
		return thread;
	}

	private static void waitForewer() {
		try {
			Thread.sleep(Integer.MAX_VALUE);
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
	}
}
