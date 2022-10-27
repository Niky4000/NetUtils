package com.servermanager.services;

import com.servermanager.StartServerManager;
import java.util.function.Consumer;
import org.junit.Test;

public class EventFileTransferTest {

	@Test
	public void test() throws Exception {
		Thread server = th(b -> StartServerManager.main(new String[]{"START", "-port", "4444", "@", "CLUSTER", "-host1", "127.0.0.1", "-home", "/home/me/tmp/shared_observable_dir", "-instanceName", "first", "-port", "48500", "-endPort", "48510", "-localPort", "48100", "-clientPort", "48200", "-clientPortRange", "10"}), "server");
		server.join(10 * 1000);
		Thread client1 = th(b -> StartServerManager.main(new String[]{"WATCH", "-host", "127.0.0.1", "-port", "4444", "-dir", "/home/me/tmp/shared_dir1", "-to", "/home/me/tmp/shared_observable_dir", "@", "EVENT", "-host", "127.0.0.1", "-port", "4444", "-dir", "/home/me/tmp/shared_dir1", "-instanceName", "firstClient", "-clientPort", "48200", "-portRange", "10"}), "client1");
//		Thread client2 = th(b -> StartServerManager.main(new String[]{"WATCH", "-host", "127.0.0.1", "-port", "4444", "-dir", "/home/me/tmp/shared_dir2", "-to", "/home/me/tmp/shared_observable_dir", "@", "EVENT", "-host", "127.0.0.1", "-port", "4444", "-dir", "/home/me/tmp/shared_dir2", "-instanceName", "firstClient", "-clientPort", "48200", "-portRange", "10"}), "client2");
		server.join();
	}

	private Thread th(Consumer<Boolean> runnable, String name) {
		Thread thread = new Thread(() -> {
			runnable.accept(true);
			waitForewer();
		});
		thread.setName(name);
		thread.start();
		return thread;
	}

	private void waitForewer() {
		try {
			Thread.sleep(Integer.MAX_VALUE);
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
	}
}
