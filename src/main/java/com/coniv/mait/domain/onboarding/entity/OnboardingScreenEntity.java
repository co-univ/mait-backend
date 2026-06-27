package com.coniv.mait.domain.onboarding.entity;

import com.coniv.mait.domain.onboarding.enums.OnboardingScreenCode;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
	name = "onboarding_screens",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_onboarding_screen_code", columnNames = {"code"})
	}
)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnboardingScreenEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private OnboardingScreenCode code;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private Boolean isExposed;

	@Enumerated(EnumType.STRING)
	private TeamUserRole targetTeamRole;

	@Builder
	private OnboardingScreenEntity(OnboardingScreenCode code, String title, Boolean isExposed,
		TeamUserRole targetTeamRole) {
		this.code = code;
		this.title = title;
		this.isExposed = isExposed;
		this.targetTeamRole = targetTeamRole;
	}

	public boolean isTargetedToAllUsers() {
		return this.targetTeamRole == null;
	}

	public void updateExposed(final Boolean isExposed) {
		this.isExposed = isExposed;
	}
}
