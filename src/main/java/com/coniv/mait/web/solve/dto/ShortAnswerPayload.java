package com.coniv.mait.web.solve.dto;

import java.util.List;

import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.service.dto.ShortAnswerDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record ShortAnswerPayload(
	@NotEmpty List<@Valid ShortAnswerDto> shortAnswers
) implements UpdateQuestionAnswerApiRequest {

	@Override
	public QuestionType type() {
		return QuestionType.SHORT;
	}
}

