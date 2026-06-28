package com.coniv.mait.domain.onboarding.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.onboarding.entity.OnboardingScreenEntity;
import com.coniv.mait.domain.onboarding.entity.UserOnboardingViewEntity;
import com.coniv.mait.domain.onboarding.entity.UserOnboardingViewId;
import com.coniv.mait.domain.onboarding.enums.OnboardingScreenCode;
import com.coniv.mait.domain.onboarding.repository.OnboardingScreenRepository;
import com.coniv.mait.domain.onboarding.repository.UserOnboardingViewRepository;
import com.coniv.mait.domain.onboarding.service.dto.OnboardingScreenDto;
import com.coniv.mait.domain.onboarding.service.dto.OnboardingViewStatusDto;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserOnboardingService {

	private final OnboardingScreenRepository onboardingScreenRepository;
	private final UserOnboardingViewRepository userOnboardingViewRepository;
	private final TeamUserEntityRepository teamUserEntityRepository;
	private final UserEntityRepository userEntityRepository;

	public List<OnboardingScreenDto> getUnviewedScreens(final Long userId) {
		Set<Long> viewedScreenIds = userOnboardingViewRepository.findAllByUserId(userId).stream()
			.map(view -> view.getId().getOnboardingScreenId())
			.collect(Collectors.toSet());
		Set<TeamUserRole> userRoles = Set.copyOf(teamUserEntityRepository.findDistinctUserRolesByUserId(userId));

		return onboardingScreenRepository.findAllByExposedTrue().stream()
			.filter(screen -> !viewedScreenIds.contains(screen.getId()))
			.filter(screen -> screen.isVisibleTo(userRoles))
			.map(OnboardingScreenDto::from)
			.toList();
	}

	public OnboardingViewStatusDto getViewStatus(final Long userId, final OnboardingScreenCode code) {
		OnboardingScreenEntity screen = onboardingScreenRepository.findByCode(code)
			.orElseThrow(() -> new EntityNotFoundException("온보딩 화면을 찾을 수 없습니다."));

		return userOnboardingViewRepository.findById(UserOnboardingViewId.of(screen.getId(), userId))
			.map(OnboardingViewStatusDto::from)
			.orElseGet(OnboardingViewStatusDto::notViewed);
	}

	@Transactional
	public void recordView(final Long userId, final OnboardingScreenCode code, final boolean dismissed) {
		OnboardingScreenEntity screen = onboardingScreenRepository.findByCode(code)
			.orElseThrow(() -> new EntityNotFoundException("온보딩 화면을 찾을 수 없습니다."));

		UserOnboardingViewId id = UserOnboardingViewId.of(screen.getId(), userId);
		Optional<UserOnboardingViewEntity> existingView = userOnboardingViewRepository.findById(id);
		if (existingView.isPresent()) {
			existingView.get().updateDismissed(dismissed);
			return;
		}

		UserEntity user = userEntityRepository.findById(userId)
			.orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

		userOnboardingViewRepository.save(UserOnboardingViewEntity.of(user, screen, LocalDateTime.now(), dismissed));
	}
}
