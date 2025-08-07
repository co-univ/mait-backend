package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.enums.QuestionStatusType;
import com.coniv.mait.domain.question.service.dto.CurrentQuestionDto;

public record CurrentQuestionApiResponse(
	Long questionSetId,
	Long questionId,
	QuestionStatusType questionStatusType
) {
	public static CurrentQuestionApiResponse from(CurrentQuestionDto dto) {
		return new CurrentQuestionApiResponse(dto.getQuestionSetId(), dto.getQuestionId(), dto.getQuestionStatus());
	}
}
