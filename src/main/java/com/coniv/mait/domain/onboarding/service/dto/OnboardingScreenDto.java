package com.coniv.mait.domain.onboarding.service.dto;

import java.time.LocalDateTime;

import com.coniv.mait.domain.onboarding.entity.OnboardingScreenEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OnboardingScreenDto {

	private final Long id;
	private final String code;
	private final String title;
	private final boolean exposed;
	private final TeamUserRole targetTeamRole;
	private final LocalDateTime createdAt;
	private final LocalDateTime modifiedAt;

	public static OnboardingScreenDto from(final OnboardingScreenEntity screen) {
		return OnboardingScreenDto.builder()
			.id(screen.getId())
			.code(screen.getCode())
			.title(screen.getTitle())
			.exposed(screen.isExposed())
			.targetTeamRole(screen.getTargetTeamRole())
			.createdAt(screen.getCreatedAt())
			.modifiedAt(screen.getModifiedAt())
			.build();
	}
}
