package com.coniv.mait.domain.solve.service.dto;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SubmitTimingDtoTest {

	@Test
	@DisplayName("'순번:시간차' 문자열을 파싱한다")
	void from_ParsesDelimitedString() {
		SubmitTimingDto dto = SubmitTimingDto.from("3:1500");

		assertThat(dto.submitOrder()).isEqualTo(3L);
		assertThat(dto.timeGapMillis()).isEqualTo(1500L);
	}

	@Test
	@DisplayName("첫 제출이면 시간차가 0으로 파싱된다")
	void from_FirstSubmit_ZeroGap() {
		SubmitTimingDto dto = SubmitTimingDto.from("1:0");

		assertThat(dto.submitOrder()).isEqualTo(1L);
		assertThat(dto.timeGapMillis()).isZero();
	}
}
