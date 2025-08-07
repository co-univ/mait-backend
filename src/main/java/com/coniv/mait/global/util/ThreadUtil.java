package com.coniv.mait.global.util;

public final class ThreadUtil {
	public static void sleep(final long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Thread was interrupted", e);
		}
	}
}
