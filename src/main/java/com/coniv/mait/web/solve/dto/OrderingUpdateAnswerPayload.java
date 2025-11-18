package com.coniv.mait.web.solve.dto;

import java.util.List;

import com.coniv.mait.domain.question.enums.QuestionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record OrderingUpdateAnswerPayload(
	@NotEmpty List<@Valid OptionOrderPatch> options
) implements UpdateAnswerPayload {

	@Override
	public QuestionType getType() {
		return QuestionType.ORDERING;
	}
}

