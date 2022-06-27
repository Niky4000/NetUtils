/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.some.tcp;

/**
 *
 * @author me
 */
public class Logger {

	private static boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

	public static void log(String message) {
		log(message, null);
	}

	public static void log(Exception e) {
		log(null, e);
	}

	public static void log(String message, Exception e) {
		if (isWindows) {
			if (message != null) {
				System.out.println(message);
			}
			if (e != null) {
				e.printStackTrace();
			}
		}
	}
}
