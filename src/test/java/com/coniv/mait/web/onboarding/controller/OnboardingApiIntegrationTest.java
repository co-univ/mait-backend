package com.coniv.mait.web.onboarding.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.coniv.mait.domain.onboarding.entity.OnboardingScreenEntity;
import com.coniv.mait.domain.onboarding.entity.UserOnboardingViewEntity;
import com.coniv.mait.domain.onboarding.enums.OnboardingScreenCode;
import com.coniv.mait.domain.onboarding.repository.OnboardingScreenRepository;
import com.coniv.mait.domain.onboarding.repository.UserOnboardingViewRepository;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@WithCustomUser
public class OnboardingApiIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private OnboardingScreenRepository onboardingScreenRepository;

	@Autowired
	private UserOnboardingViewRepository userOnboardingViewRepository;

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private TeamEntityRepository teamEntityRepository;

	@Autowired
	private TeamUserEntityRepository teamUserEntityRepository;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@BeforeEach
	void setUp() throws Exception {
		userOnboardingViewRepository.deleteAll();
		onboardingScreenRepository.deleteAll();
		Mockito.doAnswer(inv -> {
			ServletRequest request = inv.getArgument(0);
			ServletResponse response = inv.getArgument(1);
			FilterChain chain = inv.getArgument(2);
			chain.doFilter(request, response);
			return null;
		}).when(jwtAuthorizationFilter).doFilter(Mockito.any(), Mockito.any(), Mockito.any());
	}

	@Test
	@DisplayName("노출되지 않은 화면과 역할 미보유 화면은 제외하고, 전체 대상 화면만 조회한다")
	void getUnviewedScreens_filtersExposedAndRole() throws Exception {
		// given
		UserEntity user = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(TeamEntity.ofGroup("onboardingTeam", user.getId()));
		teamUserEntityRepository.save(TeamUserEntity.createPlayerUser(user, team));

		onboardingScreenRepository.save(screen(OnboardingScreenCode.HOME_GUIDE, true, null));
		onboardingScreenRepository.save(screen(OnboardingScreenCode.QUESTION_SOLVE, false, null));
		onboardingScreenRepository.save(screen(OnboardingScreenCode.QUESTION_MANAGE, true, TeamUserRole.MAKER));

		// when & then
		mockMvc.perform(get("/api/v1/onboarding/screens/unviewed"))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.length()").value(1),
				jsonPath("$.data[0].code").value("HOME_GUIDE"));
	}

	@Test
	@DisplayName("이미 본 화면은 제외하고, 소속 팀이 없는 유저는 전체 대상 화면만 조회한다")
	void getUnviewedScreens_excludesViewedForUserWithoutTeam() throws Exception {
		// given
		UserEntity user = userEntityRepository.findByEmail("user@example.com").orElseThrow();

		OnboardingScreenEntity homeGuide = onboardingScreenRepository.save(
			screen(OnboardingScreenCode.HOME_GUIDE, true, null));
		OnboardingScreenEntity questionSolve = onboardingScreenRepository.save(
			screen(OnboardingScreenCode.QUESTION_SOLVE, true, null));
		onboardingScreenRepository.save(screen(OnboardingScreenCode.QUESTION_MANAGE, true, TeamUserRole.MAKER));

		userOnboardingViewRepository.save(
			UserOnboardingViewEntity.of(user, questionSolve, LocalDateTime.now()));

		// when & then
		mockMvc.perform(get("/api/v1/onboarding/screens/unviewed"))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.length()").value(1),
				jsonPath("$.data[0].id").value(homeGuide.getId()),
				jsonPath("$.data[0].code").value("HOME_GUIDE"));
	}

	@Test
	@DisplayName("MAKER 권한만 있어도 PLAYER 대상 미열람 화면을 조회한다")
	void getUnviewedScreens_includesPlayerScreenForMakerRole() throws Exception {
		// given
		UserEntity user = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(TeamEntity.ofGroup("makerTeam", user.getId()));
		teamUserEntityRepository.save(TeamUserEntity.createTeamUser(user, team, TeamUserRole.MAKER));

		onboardingScreenRepository.save(screen(OnboardingScreenCode.QUESTION_SOLVE, true, TeamUserRole.PLAYER));

		// when & then
		mockMvc.perform(get("/api/v1/onboarding/screens/unviewed"))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.length()").value(1),
				jsonPath("$.data[0].code").value("QUESTION_SOLVE"));
	}

	@Test
	@DisplayName("특정 온보딩 화면을 열람한 경우 열람 상태를 조회한다")
	void getViewStatus_returnsViewedStatus() throws Exception {
		// given
		UserEntity user = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		OnboardingScreenEntity homeGuide = onboardingScreenRepository.save(
			screen(OnboardingScreenCode.HOME_GUIDE, true, null));
		UserOnboardingViewEntity view = UserOnboardingViewEntity.of(user, homeGuide, LocalDateTime.now());
		view.updateDismissed(true);
		userOnboardingViewRepository.save(view);

		// when & then
		mockMvc.perform(get("/api/v1/onboarding/screens/view-status")
				.param("code", "HOME_GUIDE"))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.viewed").value(true),
				jsonPath("$.data.dismissed").value(true));
	}

	@Test
	@DisplayName("MAKER 권한만 있어도 PLAYER 대상 화면의 열람 상태를 조회한다")
	void getViewStatus_returnsPlayerScreenStatusForMakerRole() throws Exception {
		// given
		UserEntity user = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(TeamEntity.ofGroup("makerTeam", user.getId()));
		teamUserEntityRepository.save(TeamUserEntity.createTeamUser(user, team, TeamUserRole.MAKER));
		onboardingScreenRepository.save(screen(OnboardingScreenCode.QUESTION_SOLVE, true, TeamUserRole.PLAYER));

		// when & then
		mockMvc.perform(get("/api/v1/onboarding/screens/view-status")
				.param("code", "QUESTION_SOLVE"))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.viewed").value(false),
				jsonPath("$.data.dismissed").value(false));
	}

	@Test
	@DisplayName("특정 온보딩 화면을 열람하지 않은 경우 미열람 상태를 조회한다")
	void getViewStatus_returnsNotViewedStatus() throws Exception {
		// given
		onboardingScreenRepository.save(screen(OnboardingScreenCode.HOME_GUIDE, true, null));

		// when & then
		mockMvc.perform(get("/api/v1/onboarding/screens/view-status")
				.param("code", "HOME_GUIDE"))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.viewed").value(false),
				jsonPath("$.data.dismissed").value(false));
	}

	@Test
	@DisplayName("특정 온보딩 화면 열람 기록을 저장하고, 중복 요청은 멱등 처리한다")
	void recordView_savesViewHistoryIdempotently() throws Exception {
		// given
		OnboardingScreenEntity homeGuide = onboardingScreenRepository.save(
			screen(OnboardingScreenCode.HOME_GUIDE, true, null));

		// when & then
		mockMvc.perform(post("/api/v1/onboarding/screens/view")
				.param("code", "HOME_GUIDE"))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data").doesNotExist());

		mockMvc.perform(post("/api/v1/onboarding/screens/view")
				.param("code", "HOME_GUIDE"))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data").doesNotExist());

		assertThat(userOnboardingViewRepository.countByOnboardingScreen_Id(homeGuide.getId())).isEqualTo(1);
	}

	private OnboardingScreenEntity screen(final OnboardingScreenCode code, final boolean exposed,
		final TeamUserRole targetTeamRole) {
		return OnboardingScreenEntity.builder()
			.code(code)
			.title(code.name())
			.exposed(exposed)
			.targetTeamRole(targetTeamRole)
			.build();
	}
}
