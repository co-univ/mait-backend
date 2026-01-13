package com.coniv.mait.domain.question.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "빈칸 채우기 문제 채점 결과")
public record GradedAnswerFillBlankResult(
	@Schema(description = "빈칸 번호", example = "1")
	Long number,

	@Schema(description = "유저가 제출한 답안", example = "정답")
	String answer,

	@Schema(description = "해당 빈칸의 정답 여부", example = "true")
	boolean isCorrect
) implements GradedAnswerResult {
}
