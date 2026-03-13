package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.service.dto.PersonalAccuracyDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PersonalAccuracyApiResponse(
	@Schema(description = "총 푼 문제 수", requiredMode = Schema.RequiredMode.REQUIRED)
	long totalSolvedCount,

	@Schema(description = "맞은 문제 수", requiredMode = Schema.RequiredMode.REQUIRED)
	long correctCount,

	@Schema(description = "정답률 (%)", requiredMode = Schema.RequiredMode.REQUIRED)
	double accuracyRate
) {
	public static PersonalAccuracyApiResponse from(PersonalAccuracyDto dto) {
		return PersonalAccuracyApiResponse.builder()
			.totalSolvedCount(dto.totalSolvedCount())
			.correctCount(dto.correctCount())
			.accuracyRate(dto.accuracyRate())
			.build();
	}
}
