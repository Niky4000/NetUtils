package com.servermanager.services;

import com.servermanager.StartServerManager;
import com.servermanager.services.bean.ClipboardObject;
import com.servermanager.services.events.ClipboardEvent;
import com.utils.ClipboardUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClipboardService extends AbstractService {

	private final StartServerManager startServerManager;

	public ClipboardService(String host, int port, StartServerManager startServerManager) {
		super(host, port);
		this.startServerManager = startServerManager;
	}

	public void createClipboardThread() {
		Thread clipboardThread = new Thread(() -> {
			ClipboardUtils.setClipboardListerner();
			String clipboardData = ClipboardUtils.getClipboardData();
			do {
				String newClipboardData = ClipboardUtils.getClipboardData();
				if (clipboardData.equals(newClipboardData)) {
					try {
						new ClientService(host, port, startServerManager).sendMessage(Arrays.asList(new ClipboardObject(new ClipboardEvent(clipboardData))).iterator());
					} catch (IOException ex) {
						Logger.getLogger(ClipboardService.class.getName()).log(Level.SEVERE, null, ex);
					} catch (ClassNotFoundException ex) {
						Logger.getLogger(ClipboardService.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			} while (true);
		});
		clipboardThread.setName("clipboardThread");
		clipboardThread.start();
	}
}
