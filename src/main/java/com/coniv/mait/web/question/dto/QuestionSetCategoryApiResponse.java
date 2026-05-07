package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.service.dto.QuestionSetCategoryDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record QuestionSetCategoryApiResponse(
	@Schema(description = "카테고리 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
	Long id,
	@Schema(description = "카테고리가 속한 팀 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
	Long teamId,
	@Schema(description = "카테고리 이름 (최대 40자)", requiredMode = Schema.RequiredMode.REQUIRED, example = "알고리즘")
	String name
) {

	public static QuestionSetCategoryApiResponse from(final QuestionSetCategoryDto dto) {
		return new QuestionSetCategoryApiResponse(dto.getId(), dto.getTeamId(), dto.getName());
	}
}
