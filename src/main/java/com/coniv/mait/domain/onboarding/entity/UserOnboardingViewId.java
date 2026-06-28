package com.coniv.mait.domain.onboarding.entity;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserOnboardingViewId implements Serializable {

	private Long onboardingScreenId;

	private Long userId;

	private UserOnboardingViewId(Long onboardingScreenId, Long userId) {
		this.onboardingScreenId = onboardingScreenId;
		this.userId = userId;
	}

	public static UserOnboardingViewId of(Long onboardingScreenId, Long userId) {
		return new UserOnboardingViewId(onboardingScreenId, userId);
	}
}
