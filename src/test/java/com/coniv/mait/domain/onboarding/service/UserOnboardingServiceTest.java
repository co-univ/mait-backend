package com.coniv.mait.domain.onboarding.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.coniv.mait.domain.onboarding.entity.OnboardingScreenEntity;
import com.coniv.mait.domain.onboarding.entity.UserOnboardingViewEntity;
import com.coniv.mait.domain.onboarding.entity.UserOnboardingViewId;
import com.coniv.mait.domain.onboarding.enums.OnboardingScreenCode;
import com.coniv.mait.domain.onboarding.repository.OnboardingScreenRepository;
import com.coniv.mait.domain.onboarding.repository.UserOnboardingViewRepository;
import com.coniv.mait.domain.onboarding.service.dto.OnboardingScreenDto;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;

@ExtendWith(MockitoExtension.class)
class UserOnboardingServiceTest {

	private static final Long USER_ID = 1L;

	@InjectMocks
	private UserOnboardingService userOnboardingService;

	@Mock
	private OnboardingScreenRepository onboardingScreenRepository;

	@Mock
	private UserOnboardingViewRepository userOnboardingViewRepository;

	@Mock
	private TeamUserEntityRepository teamUserEntityRepository;

	@Test
	@DisplayName("아직 보지 않은 노출 화면 중 전체 대상(null)과 보유 역할 대상 화면을 반환한다")
	void returnsUnviewedScreensVisibleToUser() {
		// given
		OnboardingScreenEntity allUsers = screen(1L, OnboardingScreenCode.HOME_GUIDE, null);
		OnboardingScreenEntity makerOnly = screen(2L, OnboardingScreenCode.QUESTION_MANAGE, TeamUserRole.MAKER);
		given(onboardingScreenRepository.findAllByExposedTrue()).willReturn(List.of(allUsers, makerOnly));
		given(userOnboardingViewRepository.findAllByUserId(USER_ID)).willReturn(List.of());
		given(teamUserEntityRepository.findDistinctUserRolesByUserId(USER_ID)).willReturn(List.of(TeamUserRole.MAKER));

		// when
		List<OnboardingScreenDto> result = userOnboardingService.getUnviewedScreens(USER_ID);

		// then
		assertThat(result).extracting(OnboardingScreenDto::getCode)
			.containsExactlyInAnyOrder(OnboardingScreenCode.HOME_GUIDE, OnboardingScreenCode.QUESTION_MANAGE);
	}

	@Test
	@DisplayName("이미 본 화면은 결과에서 제외한다")
	void excludesViewedScreens() {
		// given
		OnboardingScreenEntity viewed = screen(1L, OnboardingScreenCode.HOME_GUIDE, null);
		OnboardingScreenEntity unviewed = screen(2L, OnboardingScreenCode.QUESTION_SOLVE, null);
		UserOnboardingViewEntity viewedView = viewOf(1L);
		given(onboardingScreenRepository.findAllByExposedTrue()).willReturn(List.of(viewed, unviewed));
		given(userOnboardingViewRepository.findAllByUserId(USER_ID)).willReturn(List.of(viewedView));
		given(teamUserEntityRepository.findDistinctUserRolesByUserId(USER_ID)).willReturn(List.of());

		// when
		List<OnboardingScreenDto> result = userOnboardingService.getUnviewedScreens(USER_ID);

		// then
		assertThat(result).extracting(OnboardingScreenDto::getCode)
			.containsExactly(OnboardingScreenCode.QUESTION_SOLVE);
	}

	@Test
	@DisplayName("상위 역할(OWNER)은 하위 역할(MAKER) 대상 화면도 볼 수 있다")
	void higherRoleCoversLowerRoleGatedScreen() {
		// given
		OnboardingScreenEntity makerOnly = screen(1L, OnboardingScreenCode.QUESTION_MANAGE, TeamUserRole.MAKER);
		given(onboardingScreenRepository.findAllByExposedTrue()).willReturn(List.of(makerOnly));
		given(userOnboardingViewRepository.findAllByUserId(USER_ID)).willReturn(List.of());
		given(teamUserEntityRepository.findDistinctUserRolesByUserId(USER_ID)).willReturn(List.of(TeamUserRole.OWNER));

		// when
		List<OnboardingScreenDto> result = userOnboardingService.getUnviewedScreens(USER_ID);

		// then
		assertThat(result).extracting(OnboardingScreenDto::getCode)
			.containsExactly(OnboardingScreenCode.QUESTION_MANAGE);
	}

	@Test
	@DisplayName("역할 대상 화면은 유저가 해당 역할 이상을 보유하지 않으면 제외한다")
	void excludesRoleGatedScreenWhenUserLacksRole() {
		// given
		OnboardingScreenEntity allUsers = screen(1L, OnboardingScreenCode.HOME_GUIDE, null);
		OnboardingScreenEntity makerOnly = screen(2L, OnboardingScreenCode.QUESTION_MANAGE, TeamUserRole.MAKER);
		given(onboardingScreenRepository.findAllByExposedTrue()).willReturn(List.of(allUsers, makerOnly));
		given(userOnboardingViewRepository.findAllByUserId(USER_ID)).willReturn(List.of());
		given(teamUserEntityRepository.findDistinctUserRolesByUserId(USER_ID)).willReturn(List.of(TeamUserRole.PLAYER));

		// when
		List<OnboardingScreenDto> result = userOnboardingService.getUnviewedScreens(USER_ID);

		// then
		assertThat(result).extracting(OnboardingScreenDto::getCode)
			.containsExactly(OnboardingScreenCode.HOME_GUIDE);
	}

	private OnboardingScreenEntity screen(final Long id, final OnboardingScreenCode code, final TeamUserRole role) {
		OnboardingScreenEntity screen = OnboardingScreenEntity.builder()
			.code(code)
			.title(code.name())
			.exposed(true)
			.targetTeamRole(role)
			.build();
		ReflectionTestUtils.setField(screen, "id", id);
		return screen;
	}

	private UserOnboardingViewEntity viewOf(final Long screenId) {
		UserOnboardingViewEntity view = mock(UserOnboardingViewEntity.class);
		given(view.getId()).willReturn(UserOnboardingViewId.of(screenId, USER_ID));
		return view;
	}
}
