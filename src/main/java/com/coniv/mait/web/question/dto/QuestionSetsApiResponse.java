package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.enums.DeliveryMode;

import lombok.Builder;

@Builder
public record QuestionSetsApiResponse(
	DeliveryMode mode,
	QuestionSetContainer questionSets
) {

	public static QuestionSetsApiResponse of(DeliveryMode mode, QuestionSetContainer questionSets) {
		return QuestionSetsApiResponse.builder()
			.mode(mode)
			.questionSets(questionSets)
			.build();
	}
}
