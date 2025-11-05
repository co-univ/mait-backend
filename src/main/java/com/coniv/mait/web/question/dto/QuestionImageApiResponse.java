package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.service.dto.QuestionImageDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record QuestionImageApiResponse(
	@Schema(description = "문제 이미지 아이디", requiredMode = Schema.RequiredMode.REQUIRED)
	Long id,
	@Schema(description = "생성된 이미지 url", requiredMode = Schema.RequiredMode.REQUIRED)
	String imageUrl
) {
	public static QuestionImageApiResponse from(QuestionImageDto questionImageDto) {
		return QuestionImageApiResponse.builder()
			.id(questionImageDto.getId())
			.imageUrl(questionImageDto.getImageUrl())
			.build();
	}
}
