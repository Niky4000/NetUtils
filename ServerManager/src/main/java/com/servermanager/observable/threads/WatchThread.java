package com.servermanager.observable.threads;

import static com.utils.Logger.println;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class WatchThread extends Thread implements InterruptableThread {

	private final Consumer<Set<File>> consumer;
	private final AtomicInteger modificationCounter;
	private static final long TIME_TO_WAIT = 2L * 1000L;
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
					println(e);
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
