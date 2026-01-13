package com.coniv.mait.domain.question.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "객관식 문제 채점 결과")
public record GradedAnswerMultipleResult(
	@Schema(description = "선택지 번호", example = "1")
	Long number,

	@Schema(description = "해당 선택지의 정답 여부", example = "true")
	boolean isCorrect
) implements GradedAnswerResult {
}
