package com.coniv.mait.web.solve.dto;

import java.util.Set;

import com.coniv.mait.domain.question.enums.QuestionType;

import jakarta.validation.constraints.NotEmpty;

public record MultipleChoiceUpdateAnswerPayload(
	@NotEmpty Set<Long> correctChoiceIds
) implements UpdateAnswerPayload {

	@Override
	public QuestionType getType() {
		return QuestionType.MULTIPLE;
	}
}

