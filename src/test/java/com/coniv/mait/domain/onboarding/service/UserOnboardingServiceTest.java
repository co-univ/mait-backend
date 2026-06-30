package com.coniv.mait.domain.onboarding.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.coniv.mait.domain.onboarding.entity.OnboardingScreenEntity;
import com.coniv.mait.domain.onboarding.entity.UserOnboardingViewEntity;
import com.coniv.mait.domain.onboarding.entity.UserOnboardingViewId;
import com.coniv.mait.domain.onboarding.repository.OnboardingScreenRepository;
import com.coniv.mait.domain.onboarding.repository.UserOnboardingViewRepository;
import com.coniv.mait.domain.onboarding.service.dto.OnboardingScreenDto;
import com.coniv.mait.domain.onboarding.service.dto.OnboardingViewStatusDto;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.service.component.UserReader;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserOnboardingServiceTest {

	private static final Long USER_ID = 1L;
	private static final Long SCREEN_ID = 1L;

	@InjectMocks
	private UserOnboardingService userOnboardingService;

	@Mock
	private OnboardingScreenRepository onboardingScreenRepository;

	@Mock
	private UserOnboardingViewRepository userOnboardingViewRepository;

	@Mock
	private TeamUserEntityRepository teamUserEntityRepository;

	@Mock
	private UserReader userReader;

	@Test
	@DisplayName("아직 보지 않은 노출 화면 중 전체 대상(null)과 보유 역할 대상 화면을 반환한다")
	void returnsUnviewedScreensVisibleToUser() {
		// given
		OnboardingScreenEntity allUsers = screen(1L, "HOME_GUIDE", null);
		OnboardingScreenEntity makerOnly = screen(2L, "QUESTION_MANAGE", TeamUserRole.MAKER);
		given(onboardingScreenRepository.findAllByExposedTrue()).willReturn(List.of(allUsers, makerOnly));
		given(userOnboardingViewRepository.findAllByUserId(USER_ID)).willReturn(List.of());
		given(teamUserEntityRepository.findDistinctUserRolesByUserId(USER_ID)).willReturn(List.of(TeamUserRole.MAKER));

		// when
		List<OnboardingScreenDto> result = userOnboardingService.getUnviewedScreens(USER_ID);

		// then
		assertThat(result).extracting(OnboardingScreenDto::getCode)
			.containsExactlyInAnyOrder("HOME_GUIDE", "QUESTION_MANAGE");
	}

	@Test
	@DisplayName("이미 본 화면은 결과에서 제외한다")
	void excludesViewedScreens() {
		// given
		OnboardingScreenEntity viewed = screen(1L, "HOME_GUIDE", null);
		OnboardingScreenEntity unviewed = screen(2L, "QUESTION_SOLVE", null);
		UserOnboardingViewEntity viewedView = viewOf(1L);
		given(onboardingScreenRepository.findAllByExposedTrue()).willReturn(List.of(viewed, unviewed));
		given(userOnboardingViewRepository.findAllByUserId(USER_ID)).willReturn(List.of(viewedView));
		given(teamUserEntityRepository.findDistinctUserRolesByUserId(USER_ID)).willReturn(List.of());

		// when
		List<OnboardingScreenDto> result = userOnboardingService.getUnviewedScreens(USER_ID);

		// then
		assertThat(result).extracting(OnboardingScreenDto::getCode)
			.containsExactly("QUESTION_SOLVE");
	}

	@Test
	@DisplayName("상위 역할(OWNER)은 하위 역할(MAKER) 대상 화면도 볼 수 있다")
	void higherRoleCoversLowerRoleGatedScreen() {
		// given
		OnboardingScreenEntity makerOnly = screen(1L, "QUESTION_MANAGE", TeamUserRole.MAKER);
		given(onboardingScreenRepository.findAllByExposedTrue()).willReturn(List.of(makerOnly));
		given(userOnboardingViewRepository.findAllByUserId(USER_ID)).willReturn(List.of());
		given(teamUserEntityRepository.findDistinctUserRolesByUserId(USER_ID)).willReturn(List.of(TeamUserRole.OWNER));

		// when
		List<OnboardingScreenDto> result = userOnboardingService.getUnviewedScreens(USER_ID);

		// then
		assertThat(result).extracting(OnboardingScreenDto::getCode)
			.containsExactly("QUESTION_MANAGE");
	}

	@Test
	@DisplayName("OWNER 역할은 PLAYER 대상 화면도 볼 수 있다")
	void ownerRoleCoversPlayerGatedScreen() {
		// given
		OnboardingScreenEntity playerOnly = screen(1L, "QUESTION_SOLVE", TeamUserRole.PLAYER);
		given(onboardingScreenRepository.findAllByExposedTrue()).willReturn(List.of(playerOnly));
		given(userOnboardingViewRepository.findAllByUserId(USER_ID)).willReturn(List.of());
		given(teamUserEntityRepository.findDistinctUserRolesByUserId(USER_ID)).willReturn(List.of(TeamUserRole.OWNER));

		// when
		List<OnboardingScreenDto> result = userOnboardingService.getUnviewedScreens(USER_ID);

		// then
		assertThat(result).extracting(OnboardingScreenDto::getCode)
			.containsExactly("QUESTION_SOLVE");
	}

	@Test
	@DisplayName("역할 대상 화면은 유저가 해당 역할 이상을 보유하지 않으면 제외한다")
	void excludesRoleGatedScreenWhenUserLacksRole() {
		// given
		OnboardingScreenEntity allUsers = screen(1L, "HOME_GUIDE", null);
		OnboardingScreenEntity makerOnly = screen(2L, "QUESTION_MANAGE", TeamUserRole.MAKER);
		given(onboardingScreenRepository.findAllByExposedTrue()).willReturn(List.of(allUsers, makerOnly));
		given(userOnboardingViewRepository.findAllByUserId(USER_ID)).willReturn(List.of());
		given(teamUserEntityRepository.findDistinctUserRolesByUserId(USER_ID)).willReturn(List.of(TeamUserRole.PLAYER));

		// when
		List<OnboardingScreenDto> result = userOnboardingService.getUnviewedScreens(USER_ID);

		// then
		assertThat(result).extracting(OnboardingScreenDto::getCode)
			.containsExactly("HOME_GUIDE");
	}

	@Test
	@DisplayName("온보딩 화면을 열람한 경우 열람 상태를 반환한다")
	void getViewStatus_returnsViewedStatus() {
		// given
		UserOnboardingViewEntity view = dismissedViewOf(true);
		given(onboardingScreenRepository.existsById(SCREEN_ID)).willReturn(true);
		given(userOnboardingViewRepository.findById(UserOnboardingViewId.of(SCREEN_ID, USER_ID)))
			.willReturn(Optional.of(view));

		// when
		OnboardingViewStatusDto result = userOnboardingService.getViewStatus(USER_ID, SCREEN_ID);

		// then
		assertThat(result.viewed()).isTrue();
		assertThat(result.dismissed()).isTrue();
	}

	@Test
	@DisplayName("온보딩 화면을 열람하지 않은 경우 미열람 상태를 반환한다")
	void getViewStatus_returnsNotViewedStatus() {
		// given
		given(onboardingScreenRepository.existsById(SCREEN_ID)).willReturn(true);
		given(userOnboardingViewRepository.findById(UserOnboardingViewId.of(SCREEN_ID, USER_ID)))
			.willReturn(Optional.empty());

		// when
		OnboardingViewStatusDto result = userOnboardingService.getViewStatus(USER_ID, SCREEN_ID);

		// then
		assertThat(result.viewed()).isFalse();
		assertThat(result.dismissed()).isFalse();
	}

	@Test
	@DisplayName("온보딩 화면 ID가 존재하지 않으면 예외가 발생한다")
	void getViewStatus_throwsExceptionWhenScreenNotFound() {
		// given
		given(onboardingScreenRepository.existsById(SCREEN_ID)).willReturn(false);

		// when & then
		assertThatThrownBy(() -> userOnboardingService.getViewStatus(USER_ID, SCREEN_ID))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage("온보딩 화면을 찾을 수 없습니다.");
		then(userOnboardingViewRepository).should(never()).findById(any());
	}

	@Test
	@DisplayName("온보딩 화면 열람 기록을 저장한다")
	void recordView_savesViewHistory() {
		// given
		OnboardingScreenEntity screen = screen(SCREEN_ID, "HOME_GUIDE", null);
		UserEntity user = user();
		given(onboardingScreenRepository.findById(SCREEN_ID)).willReturn(Optional.of(screen));
		given(userOnboardingViewRepository.findById(UserOnboardingViewId.of(SCREEN_ID, USER_ID)))
			.willReturn(Optional.empty());
		given(userReader.getById(USER_ID)).willReturn(user);

		// when
		userOnboardingService.recordView(USER_ID, SCREEN_ID, true);

		// then
		ArgumentCaptor<UserOnboardingViewEntity> viewCaptor = ArgumentCaptor.forClass(UserOnboardingViewEntity.class);
		then(userOnboardingViewRepository).should().save(viewCaptor.capture());
		assertThat(viewCaptor.getValue().isDismissed()).isTrue();
	}

	@Test
	@DisplayName("이미 열람한 온보딩 화면이면 다시 보지 않기 여부를 갱신한다")
	void recordView_updatesDismissedWhenAlreadyViewed() {
		// given
		OnboardingScreenEntity screen = screen(SCREEN_ID, "HOME_GUIDE", null);
		UserOnboardingViewEntity view = mock(UserOnboardingViewEntity.class);
		given(onboardingScreenRepository.findById(SCREEN_ID)).willReturn(Optional.of(screen));
		given(userOnboardingViewRepository.findById(UserOnboardingViewId.of(SCREEN_ID, USER_ID)))
			.willReturn(Optional.of(view));

		// when
		userOnboardingService.recordView(USER_ID, SCREEN_ID, true);

		// then
		then(view).should().updateDismissed(true);
		then(userReader).should(never()).getById(any());
		then(userOnboardingViewRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("온보딩 열람 이력 초기화 시 해당 유저의 전체 열람 기록을 삭제한다")
	void resetViewHistory_deletesAllViews() {
		// when
		userOnboardingService.resetViewHistory(USER_ID);

		// then
		then(userOnboardingViewRepository).should().deleteAllByUserId(USER_ID);
	}

	@Test
	@DisplayName("열람 기록 저장 시 온보딩 화면 ID가 존재하지 않으면 예외가 발생한다")
	void recordView_throwsExceptionWhenScreenNotFound() {
		// given
		given(onboardingScreenRepository.findById(SCREEN_ID)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userOnboardingService.recordView(USER_ID, SCREEN_ID, false))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage("온보딩 화면을 찾을 수 없습니다.");
		then(userOnboardingViewRepository).should(never()).save(any());
	}

	private OnboardingScreenEntity screen(final Long id, final String code, final TeamUserRole role) {
		OnboardingScreenEntity screen = OnboardingScreenEntity.builder()
			.code(code)
			.title(code)
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

	private UserEntity user() {
		UserEntity user = mock(UserEntity.class);
		given(user.getId()).willReturn(USER_ID);
		return user;
	}
}
