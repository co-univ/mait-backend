package com.coniv.mait.global.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MathUtilTest {

	@ParameterizedTest
	@CsvSource({
		"7, 10, 70.0",
		"1, 3, 33.3",
		"2, 3, 66.7",
		"1, 1, 100.0",
		"0, 10, 0.0",
		"1, 7, 14.3",
		"10, 10, 100.0",
		"999, 1000, 99.9"
	})
	@DisplayName("정답률 계산 - 소수 첫째자리 반올림 검증")
	void calculateRateRoundedToFirstDecimal_ShouldRoundToFirstDecimal(long numerator, long denominator,
		double expected) {
		// when
		double result = MathUtil.calculateRateRoundedToFirstDecimal(numerator, denominator);

		// then
		assertEquals(expected, result);
	}

	@Test
	@DisplayName("분모가 0일 때 0.0 반환")
	void calculateRateRoundedToFirstDecimal_WithZeroDenominator_ShouldReturnZero() {
		// when
		double result = MathUtil.calculateRateRoundedToFirstDecimal(5, 0);

		// then
		assertEquals(0.0, result);
	}

	@Test
	@DisplayName("분자와 분모가 모두 0일 때 0.0 반환")
	void calculateRateRoundedToFirstDecimal_WithBothZero_ShouldReturnZero() {
		// when
		double result = MathUtil.calculateRateRoundedToFirstDecimal(0, 0);

		// then
		assertEquals(0.0, result);
	}
}
