package com.coniv.mait.global.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MathUtil {

	private static final int FIRST_DECIMAL_PLACE = 1;
	private static final int DIVISION_PRECISION = FIRST_DECIMAL_PLACE + 2;

	private MathUtil() {
	}

	public static double calculateRateRoundedToFirstDecimal(long numerator, long denominator) {
		if (denominator == 0) {
			return 0.0;
		}
		return BigDecimal.valueOf(numerator)
			.divide(BigDecimal.valueOf(denominator), DIVISION_PRECISION, RoundingMode.HALF_UP)
			.multiply(BigDecimal.valueOf(100))
			.setScale(FIRST_DECIMAL_PLACE, RoundingMode.HALF_UP)
			.doubleValue();
	}
}
