package com.coniv.mait.web.solve.dto;

import java.util.List;

import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.service.dto.FillBlankAnswerDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record FillBlankAnswerPayload(
	@NotEmpty List<@Valid FillBlankAnswerDto> answers
) implements UpdateQuestionAnswerApiRequest {

	@Override
	public QuestionType type() {
		return QuestionType.FILL_BLANK;
	}
}

