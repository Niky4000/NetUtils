package com.servermanager.services;

import com.servermanager.StartServerManager;
import com.servermanager.services.bean.ClipboardObject;
import com.servermanager.services.events.ClipboardEvent;
import com.utils.ClipboardUtils;
import com.utils.WaitUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClipboardService extends AbstractService {

	private final StartServerManager startServerManager;
	private static final int SECONDS_TO_WAIT = 2;

	public ClipboardService(String host, int port, StartServerManager startServerManager) {
		super(host, port);
		this.startServerManager = startServerManager;
	}

	public void createClipboardThread() {
		Thread clipboardThread = new Thread(() -> {
			String clipboardData = ClipboardUtils.getClipboardData();
			do {
				WaitUtils.waitSomeTime(SECONDS_TO_WAIT);
				String newClipboardData = ClipboardUtils.getClipboardData();
				if (!clipboardData.equals(newClipboardData)) {
					try {
						clipboardData = newClipboardData;
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
		ClipboardUtils.setClipboardListerner(clipboardThread);
		clipboardThread.start();
	}
}
