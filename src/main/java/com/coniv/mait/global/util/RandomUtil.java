package com.coniv.mait.global.util;

import java.util.concurrent.ThreadLocalRandom;

public final class RandomUtil {

	private RandomUtil() {
	}

	public static int getRandomNumber(final int max) {
		if (max <= 0) {
			throw new IllegalArgumentException("최대값은 0보다 커야 합니다.");
		}
		return ThreadLocalRandom.current().nextInt(max);
	}
}
