package com.coniv.mait.domain.question.service.dto;

import com.coniv.mait.domain.question.enums.QuestionType;

public record QuestionTypeCount(
	QuestionType type,
	long count
) {
	public static QuestionTypeCount of(final QuestionType type, final long count) {
		return new QuestionTypeCount(type, count);
	}
}
