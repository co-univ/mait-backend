package com.coniv.mait.web.question.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateQuestionSetFieldApiRequest(
	@NotBlank(message = "문제 셋 제목은 비어있을 수 없습니다.")
	String title
) {
}
