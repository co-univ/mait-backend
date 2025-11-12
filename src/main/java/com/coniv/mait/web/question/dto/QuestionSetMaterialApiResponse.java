package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.service.dto.QuestionSetMaterialDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record QuestionSetMaterialApiResponse(
	@Schema(description = "업로드한 자료 PK", requiredMode = Schema.RequiredMode.REQUIRED)
	Long id,

	@Schema(description = "업로드된 자료가 저장된 url", requiredMode = Schema.RequiredMode.REQUIRED)
	String materialUrl
) {

	public static QuestionSetMaterialApiResponse from(QuestionSetMaterialDto questionSetMaterialDto) {
		return QuestionSetMaterialApiResponse.builder()
			.id(questionSetMaterialDto.getId())
			.materialUrl(questionSetMaterialDto.getMaterialUrl())
			.build();
	}
}
