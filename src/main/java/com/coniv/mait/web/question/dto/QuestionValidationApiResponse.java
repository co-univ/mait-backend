package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.service.dto.QuestionValidateDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record QuestionValidationApiResponse(
	@Schema(description = "문제 PK", requiredMode = Schema.RequiredMode.REQUIRED)
	Long questionId,
	@Schema(description = "유효성 검사 여부", requiredMode = Schema.RequiredMode.REQUIRED)
	boolean isValid,
	@Schema(description = "문제 번호, 존재하지 않을 수 있음", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	Long number
) {

	public static QuestionValidationApiResponse from(QuestionValidateDto dto) {
		return QuestionValidationApiResponse.builder()
			.questionId(dto.getQuestionId())
			.isValid(dto.isValid())
			.number(dto.getNumber())
			.build();
	}
}
