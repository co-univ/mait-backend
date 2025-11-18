package com.coniv.mait.web.solve.dto;

import java.util.List;

import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.service.dto.ShortAnswerDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record ShortUpdateAnswerPayload(
	@NotEmpty List<@Valid ShortAnswerDto> shortAnswers
) implements UpdateAnswerPayload {

	@Override
	public QuestionType getType() {
		return QuestionType.SHORT;
	}
}

