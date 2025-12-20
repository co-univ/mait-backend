package com.coniv.mait.web.question.dto;

import jakarta.validation.constraints.NotNull;

public record LastViewedQuestionApiRequest(
	@NotNull(message = "마지막으로 조회한 문제 PK를 입력해주세요")
	Long questionId
) {
}
