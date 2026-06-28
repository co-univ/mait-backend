package com.coniv.mait.domain.onboarding.entity;

import java.time.LocalDateTime;

import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "user_onboarding_views")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserOnboardingViewEntity extends BaseTimeEntity {

	@EmbeddedId
	private UserOnboardingViewId id;

	@MapsId("onboardingScreenId")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "onboarding_screen_id", nullable = false)
	private OnboardingScreenEntity onboardingScreen;

	@MapsId("userId")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	@Column(nullable = false)
	private LocalDateTime viewedAt;

	@Column(nullable = false)
	private boolean dismissed;

	private UserOnboardingViewEntity(UserEntity user, OnboardingScreenEntity onboardingScreen,
		LocalDateTime viewedAt, boolean dismissed) {
		this.id = UserOnboardingViewId.of(onboardingScreen.getId(), user.getId());
		this.user = user;
		this.onboardingScreen = onboardingScreen;
		this.viewedAt = viewedAt;
		this.dismissed = dismissed;
	}

	public static UserOnboardingViewEntity of(UserEntity user, OnboardingScreenEntity onboardingScreen,
		LocalDateTime viewedAt) {
		return new UserOnboardingViewEntity(user, onboardingScreen, viewedAt, false);
	}

	public static UserOnboardingViewEntity of(UserEntity user, OnboardingScreenEntity onboardingScreen,
		LocalDateTime viewedAt, boolean dismissed) {
		return new UserOnboardingViewEntity(user, onboardingScreen, viewedAt, dismissed);
	}

	public void updateDismissed(final boolean dismissed) {
		this.dismissed = dismissed;
	}
}
