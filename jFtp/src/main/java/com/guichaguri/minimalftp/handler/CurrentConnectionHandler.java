package com.guichaguri.minimalftp.handler;

import com.guichaguri.minimalftp.FTPServer;
import com.guichaguri.minimalftp.Utils;
import com.guichaguri.minimalftp.handler.bean.ServerSocketBean;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.AbstractMap;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class CurrentConnectionHandler extends Thread {

	private final FTPServer ftpServer;
	private final BlockingQueue<Integer> portQueue;
	private ConcurrentSkipListMap<Date, Entry<String, Supplier<ServerSocket>>> closeMap = new ConcurrentSkipListMap<>();
	private ConcurrentSkipListMap<String, Date> ipMap = new ConcurrentSkipListMap<>();
	private ConcurrentSkipListMap<String, ServerSocketBean> serverSocketMap = new ConcurrentSkipListMap<>();
	private static final int timeToWait = 10 * 1000;
	private static final int connectionTimeToLive = -60;

	public CurrentConnectionHandler(FTPServer ftpServer, Integer lowerBound, Integer upperBound) {
		super("CloseOldConnections");
		this.ftpServer = ftpServer;
		portQueue = new ArrayBlockingQueue<>(upperBound - lowerBound + 1);
		IntStream.rangeClosed(lowerBound, upperBound).forEach(i -> portQueue.add(i));
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(timeToWait);
			} catch (InterruptedException ex) {
				continue;
			}
			Calendar calendar = Calendar.getInstance(); // gets a calendar using the default time zone and locale.
			calendar.add(Calendar.SECOND, connectionTimeToLive);
			ConcurrentNavigableMap<Date, Entry<String, Supplier<ServerSocket>>> headMap = closeMap.headMap(calendar.getTime());
			if (!headMap.isEmpty()) {
				Iterator<Entry<Date, Entry<String, Supplier<ServerSocket>>>> iterator = headMap.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<Date, Entry<String, Supplier<ServerSocket>>> entry = iterator.next();
					Entry<String, Supplier<ServerSocket>> passiveServerEntry = entry.getValue();
					String ip = passiveServerEntry.getKey();
					Supplier<ServerSocket> passiveServerSupplier = passiveServerEntry.getValue();
					serverSocketMap.compute(ip, (ip_, serverSocketBean_) -> {
						passiveServerSupplier.get();
						ipMap.remove(ip);
						iterator.remove();
						return null;
					});
				}
			}
		}
	}

	public ServerSocketBean getPort(String ip, Consumer<ServerSocket> passiveServerConsumer, boolean secureData) throws IOException {
		ServerSocketBean serverSocketBean_ = null;
		do {
			serverSocketBean_ = serverSocketMap.computeIfAbsent(ip, ip_ -> {
				final AtomicInteger newport = new AtomicInteger(-1);
				while (true) {
					try {
						newport.set(portQueue.take());
						break;
					} catch (InterruptedException ex) {
						continue;
					}
				}
				ServerSocket passiveServer;
				try {
					passiveServer = Utils.createServer(newport.get(), 5, ftpServer.getAddress(), ftpServer.getSSLContext(), secureData);
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
				Date date = new Date();
				closeMap.put(date, new AbstractMap.SimpleEntry<>(ip, () -> {
					passiveServerConsumer.accept(passiveServer);
					while (newport.get() > 0) {
						try {
							portQueue.put(newport.get());
							break;
						} catch (InterruptedException ex) {
							Logger.getLogger(CurrentConnectionHandler.class.getName()).log(Level.SEVERE, null, ex);
						}
					}
					return null;
				}));
				ipMap.put(ip, date);
				ServerSocketBean serverSocketBean = new ServerSocketBean(newport.get(), date, passiveServer);
				return serverSocketBean;
			});
		} while (serverSocketBean_ == null);
		ipMap.computeIfPresent(ip, (ip_, date) -> {
			date.setTime(new Date().getTime());
			return date;
		});
		return serverSocketBean_;
	}
}
