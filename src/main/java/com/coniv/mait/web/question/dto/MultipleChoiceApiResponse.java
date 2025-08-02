package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.service.dto.MultipleChoiceDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MultipleChoiceApiResponse(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	Long id,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	int number,
	String content,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	boolean isCorrect
) {
	public static MultipleChoiceApiResponse from(MultipleChoiceDto dto) {
		return new MultipleChoiceApiResponse(
			dto.getId(),
			dto.getNumber(),
			dto.getContent(),
			dto.isCorrect()
		);
	}
}
