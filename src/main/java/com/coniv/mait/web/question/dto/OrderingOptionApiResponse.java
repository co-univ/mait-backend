package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.service.dto.OrderingQuestionOptionDto;

public record OrderingOptionApiResponse(
	int originOrder,
	String content,
	int answerOrder
) {
	public static OrderingOptionApiResponse from(OrderingQuestionOptionDto dto) {
		return new OrderingOptionApiResponse(
			dto.getOriginOrder(),
			dto.getContent(),
			dto.getAnswerOrder()
		);
	}
}