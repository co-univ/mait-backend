package com.coniv.mait.web.solve.dto;

import java.util.List;

import com.coniv.mait.domain.question.enums.QuestionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record OrderingAnswerPayload(
	@NotEmpty List<@Valid OptionOrderPatch> optionOrders
) implements UpdateQuestionAnswerApiRequest {

	@Override
	public QuestionType type() {
		return QuestionType.ORDERING;
	}
}

