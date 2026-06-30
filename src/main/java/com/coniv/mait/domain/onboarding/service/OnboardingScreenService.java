package com.coniv.mait.domain.onboarding.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.onboarding.entity.OnboardingScreenEntity;
import com.coniv.mait.domain.onboarding.repository.OnboardingScreenRepository;
import com.coniv.mait.domain.onboarding.service.dto.OnboardingScreenDto;
import com.coniv.mait.domain.team.enums.TeamUserRole;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OnboardingScreenService {

	private final OnboardingScreenRepository onboardingScreenRepository;

	@Transactional
	public OnboardingScreenDto uploadScreen(final String code, final String title,
		final TeamUserRole targetTeamRole) {
		return onboardingScreenRepository.findByCode(code)
			.map(screen -> {
				screen.update(title, targetTeamRole);
				return OnboardingScreenDto.from(screen);
			})
			.orElseGet(() -> createScreen(code, title, targetTeamRole));
	}

	private OnboardingScreenDto createScreen(final String code, final String title, final TeamUserRole targetTeamRole) {
		OnboardingScreenEntity screen = onboardingScreenRepository.save(
			OnboardingScreenEntity.builder()
				.code(code)
				.title(title)
				.exposed(true)
				.targetTeamRole(targetTeamRole)
				.build()
		);
		return OnboardingScreenDto.from(screen);
	}
}
