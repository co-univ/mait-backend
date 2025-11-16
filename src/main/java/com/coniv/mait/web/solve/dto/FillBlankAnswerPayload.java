package com.coniv.mait.web.solve.dto;

import java.util.List;

import com.coniv.mait.domain.question.enums.QuestionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record FillBlankAnswerPayload(
	@NotNull QuestionType type,
	@NotEmpty List<@Valid BlankAcceptedAnswerPatch> blanks
) implements AnswerUpdatePayload {

	public FillBlankAnswerPayload {
		if (type != QuestionType.FILL_BLANK) {
			throw new IllegalArgumentException("FillBlankAnswerAppendPatch는 QuestionType.FILL_BLANK만 지원합니다.");
		}
	}
}

