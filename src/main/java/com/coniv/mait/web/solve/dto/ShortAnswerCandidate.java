package com.coniv.mait.web.solve.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ShortAnswerCandidate(
	@Schema(description = "주관식 정답 추가, 현재 스펙상 인정 답안만 추가 가능하므로 false")
	boolean main,
	@NotBlank String answer,
	@Positive Long number
) {
}

