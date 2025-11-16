package com.coniv.mait.web.solve.dto;

import java.util.List;

import com.coniv.mait.domain.question.enums.QuestionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record OrderingAnswerPayload(
	@NotNull QuestionType type,
	@NotEmpty List<@Valid OptionOrderPatch> optionOrders
) implements AnswerUpdatePayload {

	public OrderingAnswerPayload {
		if (type != QuestionType.ORDERING) {
			throw new IllegalArgumentException("OrderingAnswerPatch는 QuestionType.ORDERING만 지원합니다.");
		}
	}
}

