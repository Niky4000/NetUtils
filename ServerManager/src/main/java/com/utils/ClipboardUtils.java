package com.utils;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClipboardUtils {

	public static void setClipboard(String string) {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(string), null);
	}

	public static void getClipboard() {
		Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener((FlavorEvent e) -> {
			try {
				String data = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
//				System.out.println("ClipBoard UPDATED: " + e.getSource() + " " + e.toString() + " data: " + data);
				System.out.println(data);
			} catch (Exception ex) {
				System.out.println("Exception!");
			}
		});
//		
		try {
			String data = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
			System.out.println(data);
		} catch (UnsupportedFlavorException ex) {
			Logger.getLogger(ClipboardUtils.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(ClipboardUtils.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
