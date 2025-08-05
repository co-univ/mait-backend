package com.coniv.mait.web.solve.dto;

import com.coniv.mait.domain.solve.service.dto.AnswerSubmitDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record QuestionAnswerSubmitApiResponse(
	@Schema(description = "제출한 기록 PK", requiredMode = Schema.RequiredMode.REQUIRED)
	Long id,
	@Schema(description = "제출한 유저 PK", requiredMode = Schema.RequiredMode.REQUIRED)
	Long userId,
	@Schema(description = "제출한 문제 PK", requiredMode = Schema.RequiredMode.REQUIRED)
	Long questionId,
	@Schema(description = "정/오답 여부", requiredMode = Schema.RequiredMode.REQUIRED)
	boolean isCorrect
) {

	public static QuestionAnswerSubmitApiResponse from(AnswerSubmitDto answerSubmitDto) {
		return QuestionAnswerSubmitApiResponse.builder()
			.id(answerSubmitDto.getId())
			.userId(answerSubmitDto.getUserId())
			.questionId(answerSubmitDto.getQuestionId())
			.isCorrect(answerSubmitDto.isCorrect())
			.build();
	}
}
