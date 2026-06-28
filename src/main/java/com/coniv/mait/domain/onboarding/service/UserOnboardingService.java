package com.coniv.mait.domain.onboarding.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.coniv.mait.domain.onboarding.entity.OnboardingScreenEntity;
import com.coniv.mait.domain.onboarding.repository.OnboardingScreenRepository;
import com.coniv.mait.domain.onboarding.repository.UserOnboardingViewRepository;
import com.coniv.mait.domain.onboarding.service.dto.OnboardingScreenDto;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserOnboardingService {

	private final OnboardingScreenRepository onboardingScreenRepository;
	private final UserOnboardingViewRepository userOnboardingViewRepository;
	private final TeamUserEntityRepository teamUserEntityRepository;

	public List<OnboardingScreenDto> getUnviewedScreens(final Long userId) {
		Set<Long> viewedScreenIds = userOnboardingViewRepository.findAllByUserId(userId).stream()
			.map(view -> view.getId().getOnboardingScreenId())
			.collect(Collectors.toSet());
		Set<TeamUserRole> userRoles = Set.copyOf(teamUserEntityRepository.findDistinctUserRolesByUserId(userId));

		return onboardingScreenRepository.findAllByExposedTrue().stream()
			.filter(screen -> !viewedScreenIds.contains(screen.getId()))
			.filter(screen -> isVisibleToUser(screen, userRoles))
			.map(OnboardingScreenDto::from)
			.toList();
	}

	private boolean isVisibleToUser(final OnboardingScreenEntity screen, final Set<TeamUserRole> userRoles) {
		return screen.isTargetedToAllUsers() || userRoles.contains(screen.getTargetTeamRole());
	}
}
