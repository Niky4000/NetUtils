package com.utils;

public class Logger {

	private static volatile boolean log = false;

	public static void println(String str) {
		if (log) {
			System.out.println(str);
		}
	}

	public static void println(Object str) {
		if (log) {
			println(str != null ? str.toString() : "null");
		}
	}

	public static void println(Exception e) {
		if (log) {
			e.printStackTrace();
		}
	}

	public static void println(String message, Exception e) {
		if (log) {
			System.out.println(message);
			e.printStackTrace();
		}
	}

	public static void setLog(boolean log) {
		Logger.log = log;
	}
}
