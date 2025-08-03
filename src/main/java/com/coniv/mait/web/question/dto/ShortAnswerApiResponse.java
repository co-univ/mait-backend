package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.service.dto.ShortAnswerDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ShortAnswerApiResponse(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	Long id,
	String answer,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	boolean isMain,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	Long number
) {
	public static ShortAnswerApiResponse from(ShortAnswerDto dto) {
		return new ShortAnswerApiResponse(
			dto.getId(),
			dto.getAnswer(),
			dto.isMain(),
			dto.getNumber()
		);
	}
}
