package com.coniv.mait.web.onboarding.dto;

import com.coniv.mait.domain.onboarding.enums.OnboardingScreenCode;
import com.coniv.mait.domain.onboarding.service.dto.OnboardingScreenDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UnviewedOnboardingScreenApiResponse(

	@Schema(description = "온보딩 화면 ID", example = "1")
	Long id,

	@Schema(description = "온보딩 화면 코드", example = "QUESTION_SOLVE")
	OnboardingScreenCode code,

	@Schema(description = "온보딩 화면 이름", example = "문제 풀기 가이드")
	String title
) {

	public static UnviewedOnboardingScreenApiResponse from(final OnboardingScreenDto screen) {
		return new UnviewedOnboardingScreenApiResponse(screen.getId(), screen.getCode(), screen.getTitle());
	}
}
