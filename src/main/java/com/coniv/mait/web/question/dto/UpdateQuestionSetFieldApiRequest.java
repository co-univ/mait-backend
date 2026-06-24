package com.coniv.mait.web.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;

public record UpdateQuestionSetFieldApiRequest(
	@Schema(description = "문제 셋 제목")
	String title,
	@Deprecated
	@Schema(description = "문제 셋 제목(deprecated, title로 대체)", deprecated = true)
	String subject
) {
	public UpdateQuestionSetFieldApiRequest(final String title) {
		this(title, null);
	}

	@AssertTrue(message = "문제 셋 제목은 비어있을 수 없습니다.")
	private boolean isTitleProvided() {
		return hasText(title) || hasText(subject);
	}

	public String resolvedTitle() {
		if (hasText(title)) {
			return title;
		}
		return subject;
	}

	private static boolean hasText(final String value) {
		return value != null && !value.isBlank();
	}
}
