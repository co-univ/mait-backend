package com.coniv.mait.web.solve.dto;

import java.util.List;

import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.service.dto.FillBlankAnswerDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record FillBlankUpdateAnswerPayload(
	@NotEmpty List<@Valid FillBlankAnswerDto> answers
) implements UpdateAnswerPayload {

	@Override
	public QuestionType getType() {
		return QuestionType.FILL_BLANK;
	}
}

