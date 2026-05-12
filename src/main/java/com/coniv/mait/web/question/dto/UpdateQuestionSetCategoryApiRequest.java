package com.coniv.mait.web.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateQuestionSetCategoryApiRequest(
	@NotBlank(message = "카테고리 이름을 입력해주세요.")
	@Size(max = 40, message = "카테고리 이름은 40자 이하여야 합니다.")
	String name
) {
}
