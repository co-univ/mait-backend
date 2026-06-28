package com.coniv.mait.domain.onboarding.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;

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
import com.coniv.mait.domain.onboarding.service.dto.OnboardingViewStatusDto;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;

import jakarta.persistence.EntityNotFoundException;

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
	@DisplayName("관리 역할(OWNER)이라도 PLAYER 대상 화면은 보지 않는다")
	void managerRoleDoesNotCoverPlayerGatedScreen() {
		// given
		OnboardingScreenEntity playerOnly = screen(1L, OnboardingScreenCode.QUESTION_SOLVE, TeamUserRole.PLAYER);
		given(onboardingScreenRepository.findAllByExposedTrue()).willReturn(List.of(playerOnly));
		given(userOnboardingViewRepository.findAllByUserId(USER_ID)).willReturn(List.of());
		given(teamUserEntityRepository.findDistinctUserRolesByUserId(USER_ID)).willReturn(List.of(TeamUserRole.OWNER));

		// when
		List<OnboardingScreenDto> result = userOnboardingService.getUnviewedScreens(USER_ID);

		// then
		assertThat(result).isEmpty();
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

	@Test
	@DisplayName("온보딩 화면을 열람한 경우 열람 상태를 반환한다")
	void getViewStatus_returnsViewedStatus() {
		// given
		OnboardingScreenEntity screen = screen(1L, OnboardingScreenCode.HOME_GUIDE, null);
		UserOnboardingViewEntity view = dismissedViewOf(true);
		given(onboardingScreenRepository.findByCode(OnboardingScreenCode.HOME_GUIDE)).willReturn(Optional.of(screen));
		given(userOnboardingViewRepository.findById(UserOnboardingViewId.of(1L, USER_ID)))
			.willReturn(Optional.of(view));

		// when
		OnboardingViewStatusDto result = userOnboardingService.getViewStatus(USER_ID, OnboardingScreenCode.HOME_GUIDE);

		// then
		assertThat(result.viewed()).isTrue();
		assertThat(result.dismissed()).isTrue();
	}

	@Test
	@DisplayName("온보딩 화면을 열람하지 않은 경우 미열람 상태를 반환한다")
	void getViewStatus_returnsNotViewedStatus() {
		// given
		OnboardingScreenEntity screen = screen(1L, OnboardingScreenCode.HOME_GUIDE, null);
		given(onboardingScreenRepository.findByCode(OnboardingScreenCode.HOME_GUIDE)).willReturn(Optional.of(screen));
		given(userOnboardingViewRepository.findById(UserOnboardingViewId.of(1L, USER_ID))).willReturn(Optional.empty());

		// when
		OnboardingViewStatusDto result = userOnboardingService.getViewStatus(USER_ID, OnboardingScreenCode.HOME_GUIDE);

		// then
		assertThat(result.viewed()).isFalse();
		assertThat(result.dismissed()).isFalse();
	}

	@Test
	@DisplayName("온보딩 화면 코드가 존재하지 않으면 예외가 발생한다")
	void getViewStatus_throwsExceptionWhenScreenNotFound() {
		// given
		given(onboardingScreenRepository.findByCode(OnboardingScreenCode.HOME_GUIDE)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userOnboardingService.getViewStatus(USER_ID, OnboardingScreenCode.HOME_GUIDE))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage("온보딩 화면을 찾을 수 없습니다.");
		then(userOnboardingViewRepository).should(never()).findById(any());
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

	private UserOnboardingViewEntity dismissedViewOf(final boolean dismissed) {
		UserOnboardingViewEntity view = mock(UserOnboardingViewEntity.class);
		given(view.isDismissed()).willReturn(dismissed);
		return view;
	}
}
