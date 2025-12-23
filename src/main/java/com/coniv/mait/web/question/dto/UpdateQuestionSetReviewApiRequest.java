package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.enums.QuestionSetVisibility;

import jakarta.validation.constraints.NotNull;

public record UpdateQuestionSetReviewApiRequest(
	@NotNull(message = "공개 범위 선택은 필수 입니다.")
	QuestionSetVisibility visibility
) {
}
