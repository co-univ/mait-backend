package com.coniv.mait.web.solve.dto;

import java.util.List;

import com.coniv.mait.domain.question.enums.QuestionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ShortAnswerPayload(
	@NotNull QuestionType type,
	@NotEmpty List<@Valid ShortAnswerCandidate> answersToAdd
) implements AnswerUpdatePayload {

	public ShortAnswerPayload {
		if (type != QuestionType.SHORT) {
			throw new IllegalArgumentException("ShortAnswerAppendPatch는 QuestionType.SHORT만 지원합니다.");
		}
	}
}

