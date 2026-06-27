package com.coniv.mait.domain.onboarding.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.onboarding.entity.OnboardingScreenEntity;
import com.coniv.mait.domain.onboarding.enums.OnboardingScreenCode;
import com.coniv.mait.domain.onboarding.repository.OnboardingScreenRepository;
import com.coniv.mait.domain.onboarding.service.dto.OnboardingScreenDto;
import com.coniv.mait.domain.team.enums.TeamUserRole;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OnboardingScreenService {

	private static final boolean DEFAULT_EXPOSED = true;

	private final OnboardingScreenRepository onboardingScreenRepository;

	@Transactional
	public OnboardingScreenDto uploadScreen(final OnboardingScreenCode code, final String title,
		final TeamUserRole targetTeamRole) {
		OnboardingScreenEntity screen = onboardingScreenRepository.findByCode(code)
			.map(existing -> {
				existing.update(title, targetTeamRole);
				return existing;
			})
			.orElseGet(() -> onboardingScreenRepository.save(
				OnboardingScreenEntity.builder()
					.code(code)
					.title(title)
					.exposed(DEFAULT_EXPOSED)
					.targetTeamRole(targetTeamRole)
					.build()
			));
		return OnboardingScreenDto.from(screen);
	}
}
