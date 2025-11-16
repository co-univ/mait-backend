package com.coniv.mait.web.solve.dto;

import java.util.Set;

import com.coniv.mait.domain.question.enums.QuestionType;

import jakarta.validation.constraints.NotEmpty;

public record MultipleChoiceAnswerPayload(
	@NotEmpty Set<Long> correctChoiceIds
) implements UpdateQuestionAnswerApiRequest {
	@Override
	public QuestionType type() {
		return QuestionType.MULTIPLE;
	}
}

