package com.coniv.mait.web.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateQuestionSetCategoryApiRequest(
	@NotNull(message = "팀 정보는 필수 입니다.")
	Long teamId,
	@NotBlank(message = "카테고리 이름을 입력해주세요.")
	@Size(max = 40, message = "카테고리 이름은 40자 이하여야 합니다.")
	String name
) {
}
