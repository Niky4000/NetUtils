package com.servermanager.observable.threads;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class WatchThread extends Thread implements InterruptableThread {

	private static final Logger logger = Logger.getLogger(WatchThread.class.getName());

	private final Consumer<Set<File>> consumer;
	private final AtomicInteger modificationCounter;
	private static final long TIME_TO_WAIT = 10L * 1000L;
	private volatile boolean interruptedExternally = false;
	private final Set<File> modifiedFileSet = new HashSet<>();

	public WatchThread(int i, Consumer<Set<File>> consumer, AtomicInteger modificationCounter) {
		super("WatchThread_" + i);
		this.consumer = consumer;
		this.modificationCounter = modificationCounter;
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(Long.MAX_VALUE);
			} catch (InterruptedException ex) {
				if (interruptedExternally) {
					break;
				}
				Integer modifications = 0;
				do {
					modifications = modificationCounter.get();
					waitSomeTime();
					if (interruptedExternally) {
						break;
					}
				} while (modifications > 0 && !modifications.equals(modificationCounter.get()));
				if (interruptedExternally) {
					break;
				}
				modificationCounter.set(0);
				try {
					consumer.accept(getModifiedFileSet());
					clearModifiedFileSet();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public synchronized Set<File> getModifiedFileSet() {
		return new HashSet<>(modifiedFileSet);
	}

	public synchronized void addModifiedFileSet(File modifiedFile) {
		this.modifiedFileSet.add(modifiedFile);
	}

	private synchronized void clearModifiedFileSet() {
		this.modifiedFileSet.clear();
	}

	private void waitSomeTime() {
		try {
			Thread.sleep(TIME_TO_WAIT);
		} catch (InterruptedException ex) {
		}
	}

	@Override
	public void setInterruptFlag() {
		interruptedExternally = true;
	}
}
