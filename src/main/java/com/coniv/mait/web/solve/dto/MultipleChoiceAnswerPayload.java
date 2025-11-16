package com.coniv.mait.web.solve.dto;

import java.util.Set;

import com.coniv.mait.domain.question.enums.QuestionType;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record MultipleChoiceAnswerPayload(
	@NotNull QuestionType type,
	@NotEmpty Set<Long> correctChoiceIds
) implements AnswerUpdatePayload {

	public MultipleChoiceAnswerPayload {
		if (type != QuestionType.MULTIPLE) {
			throw new IllegalArgumentException("MultipleChoiceAnswerPatch는 QuestionType.MULTIPLE만 지원합니다.");
		}
	}
}

