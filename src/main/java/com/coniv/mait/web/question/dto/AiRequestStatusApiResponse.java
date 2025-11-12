package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.enums.AiRequestStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record AiRequestStatusApiResponse(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	Long questionSetId,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, enumAsRef = true)
	AiRequestStatus status
) {

	public static AiRequestStatusApiResponse of(Long questionSetId, AiRequestStatus status) {
		return new AiRequestStatusApiResponse(questionSetId, status);
	}
}
