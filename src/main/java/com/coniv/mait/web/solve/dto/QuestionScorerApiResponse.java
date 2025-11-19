package com.coniv.mait.web.solve.dto;

import com.coniv.mait.domain.solve.service.dto.QuestionScorerDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record QuestionScorerApiResponse(
	@Schema(description = "득점자 PK", requiredMode = Schema.RequiredMode.REQUIRED)
	Long id,
	@Schema(description = "문제 PK", requiredMode = Schema.RequiredMode.REQUIRED)
	Long questionId,
	@Schema(description = "문제 번호 PK", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	Long questionNumber,
	@Schema(description = "득점한 사용자 PK", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	Long userId,
	@Schema(description = "득점한 사용자 이름", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	String userName,
	@Schema(description = "득점 순서", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	Long submitOrder
) {
	public static QuestionScorerApiResponse from(QuestionScorerDto questionScorerDto) {
		return QuestionScorerApiResponse.builder()
			.id(questionScorerDto.getId())
			.questionId(questionScorerDto.getQuestionId())
			.questionNumber(questionScorerDto.getQuestionNumber())
			.userId(questionScorerDto.getUserId())
			.userName(questionScorerDto.getUserName())
			.submitOrder(questionScorerDto.getSubmitOrder())
			.build();
	}
}
