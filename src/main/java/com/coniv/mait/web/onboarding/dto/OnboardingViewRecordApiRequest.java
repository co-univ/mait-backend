package com.coniv.mait.web.onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record OnboardingViewRecordApiRequest(

	@Schema(description = "온보딩 화면 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
	@NotNull(message = "온보딩 화면 ID는 필수입니다.")
	Long screenId,

	@Schema(description = "다시 보지 않기 선택 여부", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
	@NotNull(message = "다시 보지 않기 선택 여부는 필수입니다.")
	Boolean dismissed
) {
}
