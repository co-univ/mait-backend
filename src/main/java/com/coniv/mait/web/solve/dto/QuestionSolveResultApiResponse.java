package com.coniv.mait.web.solve.dto;

import com.coniv.mait.domain.solve.service.dto.QuestionSolveResultDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record QuestionSolveResultApiResponse(
	@Schema(description = "문제 PK", requiredMode = Schema.RequiredMode.REQUIRED)
	Long questionId,

	@Schema(description = "정/오답 여부 (미응답 문제는 false)", requiredMode = Schema.RequiredMode.REQUIRED)
	boolean isCorrect,

	@Schema(description = "제출한 답안 (미응답 문제는 null)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	String submittedAnswer
) {

	public static QuestionSolveResultApiResponse from(final QuestionSolveResultDto dto) {
		return QuestionSolveResultApiResponse.builder()
			.questionId(dto.getQuestionId())
			.isCorrect(dto.isCorrect())
			.submittedAnswer(dto.getSubmittedAnswer())
			.build();
	}
}
