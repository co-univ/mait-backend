package com.coniv.mait.web.onboarding.dto;

import com.coniv.mait.domain.onboarding.enums.OnboardingScreenCode;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record OnboardingViewRecordApiRequest(

	@Schema(description = "온보딩 화면 식별 코드", requiredMode = Schema.RequiredMode.REQUIRED, enumAsRef = true)
	@NotNull(message = "온보딩 화면 코드는 필수입니다.")
	OnboardingScreenCode code,

	@Schema(description = "다시 보지 않기 선택 여부", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
	@NotNull(message = "다시 보지 않기 선택 여부는 필수입니다.")
	Boolean dismissed
) {
}
