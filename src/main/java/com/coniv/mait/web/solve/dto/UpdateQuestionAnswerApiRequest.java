package com.coniv.mait.web.solve.dto;

import com.coniv.mait.domain.question.enums.QuestionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UpdateQuestionAnswerApiRequest(
	@NotNull QuestionType type,
	@NotNull @Valid UpdateAnswerPayload payload
) {
}
