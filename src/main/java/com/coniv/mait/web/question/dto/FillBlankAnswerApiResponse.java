package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.service.dto.FillBlankAnswerDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record FillBlankAnswerApiResponse(
	@Schema(description = "빈칸 문제 답안 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	Long id,
	String answer,
	@Schema(description = "빈칸 문제 답안이 주관식인지 객관식인지 여부", requiredMode = Schema.RequiredMode.REQUIRED)
	boolean main,
	@Schema(description = "빈칸 문제 답안의 순서", requiredMode = Schema.RequiredMode.REQUIRED)
	Long number
) {
	public static FillBlankAnswerApiResponse from(FillBlankAnswerDto dto) {
		return new FillBlankAnswerApiResponse(
			dto.getId(),
			dto.getAnswer(),
			dto.isMain(),
			dto.getNumber()
		);
	}
}
