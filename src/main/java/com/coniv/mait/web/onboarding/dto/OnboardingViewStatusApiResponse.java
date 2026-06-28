package com.coniv.mait.web.onboarding.dto;

import com.coniv.mait.domain.onboarding.service.dto.OnboardingViewStatusDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record OnboardingViewStatusApiResponse(

	@Schema(description = "해당 온보딩 화면 열람 여부", example = "true")
	boolean viewed,

	@Schema(description = "다시 보지 않기 선택 여부 (열람하지 않았으면 false)", example = "false")
	boolean dismissed
) {

	public static OnboardingViewStatusApiResponse from(final OnboardingViewStatusDto status) {
		return new OnboardingViewStatusApiResponse(status.viewed(), status.dismissed());
	}
}
