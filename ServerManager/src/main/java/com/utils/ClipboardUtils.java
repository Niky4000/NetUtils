package com.utils;

import static com.utils.Logger.println;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.StringSelection;

public class ClipboardUtils {

	public static void setClipboard(String string) {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(string), null);
	}

	public static void setClipboardListerner(Thread thread) {
		Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener((FlavorEvent e) -> {
			thread.interrupt();
		});
	}

	public static String getClipboardData() {
		try {
			String data = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
			return data;
		} catch (Exception ex) {
			println("Exception!");
			return "";
		}
	}
}
