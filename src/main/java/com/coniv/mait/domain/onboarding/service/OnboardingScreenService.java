package com.coniv.mait.domain.onboarding.service;

import org.springframework.stereotype.Service;

import com.coniv.mait.domain.onboarding.entity.OnboardingScreenEntity;
import com.coniv.mait.domain.onboarding.repository.OnboardingScreenRepository;
import com.coniv.mait.domain.onboarding.service.dto.OnboardingScreenDto;
import com.coniv.mait.domain.team.enums.TeamUserRole;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OnboardingScreenService {

	private final OnboardingScreenRepository onboardingScreenRepository;

	public OnboardingScreenDto uploadScreen(final String code, final String title, final TeamUserRole targetTeamRole) {
		OnboardingScreenEntity onboardingScreen = onboardingScreenRepository.findByCode(code)
			.map(existingScreen -> {
				existingScreen.update(title, targetTeamRole);
				return existingScreen;
			})
			.orElseGet(() -> OnboardingScreenEntity.builder()
				.code(code)
				.title(title)
				.exposed(true)
				.targetTeamRole(targetTeamRole)
				.build());

		return OnboardingScreenDto.from(onboardingScreenRepository.save(onboardingScreen));
	}
}
