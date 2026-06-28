package com.coniv.mait.domain.onboarding.service.dto;

import com.coniv.mait.domain.onboarding.entity.UserOnboardingViewEntity;

public record OnboardingViewStatusDto(boolean viewed, boolean dismissed) {

	public static OnboardingViewStatusDto from(final UserOnboardingViewEntity view) {
		return new OnboardingViewStatusDto(true, view.isDismissed());
	}

	public static OnboardingViewStatusDto notViewed() {
		return new OnboardingViewStatusDto(false, false);
	}
}
