package com.utils;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.StringSelection;

public class ClipboardUtils {

	public void setClipboard(String string) {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(string), null);
	}

	public void setClipboardListerner() {
		Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener((FlavorEvent e) -> {
			String clipboardData = getClipboardData();
		});
	}

	public String getClipboardData() {
		try {
			String data = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
//				System.out.println("ClipBoard UPDATED: " + e.getSource() + " " + e.toString() + " data: " + data);
			System.out.println(data);
			return data;
		} catch (Exception ex) {
			System.out.println("Exception!");
			return "";
		}
	}
}
