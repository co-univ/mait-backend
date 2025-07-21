package com.coniv.mait.global.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RandomUtilTest {

	@Test
	@DisplayName("정상적인 최대값으로 랜덤 값 생성 - 범위 내 값 반환")
	void getRandomNumber_WithValidMax_ShouldReturnValueInRange() {
		// given
		int max = 10;

		// when & then
		for (int i = 0; i < 100; i++) {
			int randomNumber = RandomUtil.getRandomNumber(max);
			assertTrue(randomNumber >= 0, "랜덤 값은 0 이상이어야 합니다");
			assertTrue(randomNumber < max, "랜덤 값은 max 미만이어야 합니다");
		}
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 5, 10, 100})
	@DisplayName("다양한 최대값으로 랜덤 값 생성 - 모두 범위 내 값 반환")
	void getRandomNumber_WithVariousMax_ShouldReturnValueInRange(int max) {
		// when
		int randomNumber = RandomUtil.getRandomNumber(max);

		// then
		assertTrue(randomNumber >= 0, "랜덤 값은 0 이상이어야 합니다");
		assertTrue(randomNumber < max, "랜덤 값은 max 미만이어야 합니다");
	}

	@Test
	@DisplayName("0 입력 시 IllegalArgumentException 발생")
	void getRandomNumber_WithZero_ShouldThrowException() {
		// given
		int max = 0;

		// when & then
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> RandomUtil.getRandomNumber(max)
		);
		assertEquals("최대값은 0보다 커야 합니다.", exception.getMessage());
	}

	@Test
	@DisplayName("음수 입력 시 IllegalArgumentException 발생")
	void getRandomNumber_WithNegativeNumber_ShouldThrowException() {
		// given
		int max = -5;

		// when & then
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> RandomUtil.getRandomNumber(max)
		);
		assertEquals("최대값은 0보다 커야 합니다.", exception.getMessage());
	}

	@Test
	@DisplayName("max가 1일 때 항상 0 반환")
	void getRandomNumber_WithMaxOne_ShouldAlwaysReturnZero() {
		// given
		int max = 1;

		// when & then
		for (int i = 0; i < 10; i++) {
			int randomNumber = RandomUtil.getRandomNumber(max);
			assertEquals(0, randomNumber, "max가 1일 때는 항상 0을 반환해야 합니다");
		}
	}

	@Test
	@DisplayName("랜덤성 검증 - 100번 호출 시 서로 다른 값들 생성")
	void getRandomNumber_MultipleCall_ShouldGenerateVariousValues() {
		// given
		int max = 100;
		boolean hasDifferentValues = false;
		int firstValue = RandomUtil.getRandomNumber(max);

		// when
		for (int i = 0; i < 99; i++) {
			int randomNumber = RandomUtil.getRandomNumber(max);
			if (randomNumber != firstValue) {
				hasDifferentValues = true;
				break;
			}
		}

		// then
		assertTrue(hasDifferentValues, "랜덤 함수는 다양한 값들을 생성해야 합니다");
	}
}
