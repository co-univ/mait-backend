package com.coniv.mait.domain.question.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "단답형 문제 채점 결과")
public record GradedAnswerShortResult(
	@Schema(description = "유저가 제출한 답안", example = "정답")
	String answer,

	@Schema(description = "해당 답안의 정답 여부", example = "true")
	boolean isCorrect
) implements GradedAnswerResult {
}
