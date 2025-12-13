package com.servermanager.services;

import com.servermanager.StartServerManager;
import static com.servermanager.test.utils.Utils.th;
import org.junit.Test;

public class ClipboardTest {

	@Test
	public void test() throws Exception {
		Thread server = th(b -> StartServerManager.main(new String[]{"START", "-port", "4444", "@", "CLUSTER", "-host1", "127.0.0.1", "-home", "/home/me/tmp/shared_observable_dir", "-instanceName", "first", "-port", "48500", "-endPort", "48510", "-localPort", "48100", "-clientPort", "48200", "-clientPortRange", "10"}), "server");
		server.join(10 * 1000);
		Thread client = th(b -> StartServerManager.main(new String[]{"LOG", "@", "CLIPBOARD", "-host", "127.0.0.1", "-port", "4444", "@", "EVENT", "-host", "127.0.0.1", "-port", "4444", "-dir", "/home/me/tmp/shared_dir1", "-instanceName", "firstClient", "-clientPort", "48200", "-portRange", "10"}), "client");
		server.join();
	}
}
