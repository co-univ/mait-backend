package com.coniv.mait.domain.question.service.dto;

import com.coniv.mait.domain.question.enums.QuestionType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record QuestionCount(
	@NotNull(message = "문제 유형을 선택해주세요.")
	QuestionType type,
	@Positive(message = "문제 개수는 1개 이상이어야 합니다.")
	int count
) {
}
