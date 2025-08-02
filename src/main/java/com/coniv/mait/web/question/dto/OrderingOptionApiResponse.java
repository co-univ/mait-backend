package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.service.dto.OrderingQuestionOptionDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record OrderingOptionApiResponse(
	@Schema(description = "정렬 문제 옵션 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	Long id,
	@Schema(description = "정렬 문제 옵션의 원래 순서", requiredMode = Schema.RequiredMode.REQUIRED)
	int originOrder,
	String content,
	@Schema(description = "정렬 문제 옵션의 답안 순서", requiredMode = Schema.RequiredMode.REQUIRED)
	int answerOrder
) {
	public static OrderingOptionApiResponse from(OrderingQuestionOptionDto dto) {
		return new OrderingOptionApiResponse(
			dto.getId(),
			dto.getOriginOrder(),
			dto.getContent(),
			dto.getAnswerOrder()
		);
	}
}
