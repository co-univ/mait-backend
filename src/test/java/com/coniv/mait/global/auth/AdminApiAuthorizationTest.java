package com.coniv.mait.global.auth;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.coniv.mait.domain.admin.service.AdminLookupService;
import com.coniv.mait.domain.admin.service.AnalyticsEventCollectService;
import com.coniv.mait.domain.admin.service.AnalyticsEventQueryService;
import com.coniv.mait.domain.auth.service.Oauth2UserService;
import com.coniv.mait.domain.onboarding.service.OnboardingScreenService;
import com.coniv.mait.domain.onboarding.service.UserOnboardingService;
import com.coniv.mait.domain.onboarding.service.dto.OnboardingScreenDto;
import com.coniv.mait.domain.statistic.service.QuestionAnswerStatisticService;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.enums.LoginProvider;
import com.coniv.mait.domain.user.enums.UserRole;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.auth.jwt.JwtAccessDeniedHandler;
import com.coniv.mait.global.auth.jwt.JwtAuthenticationEntryPoint;
import com.coniv.mait.global.auth.jwt.JwtTokenProvider;
import com.coniv.mait.global.auth.jwt.repository.BlackListRepository;
import com.coniv.mait.global.config.SecurityConfig;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;
import com.coniv.mait.global.oauth.OAuth2SuccessHandler;
import com.coniv.mait.web.admin.controller.AdminLookupController;
import com.coniv.mait.web.admin.controller.AnalyticsAdminController;
import com.coniv.mait.web.admin.controller.AnalyticsEventController;
import com.coniv.mait.web.admin.controller.OnboardingAdminController;
import com.coniv.mait.web.admin.controller.OnboardingDevAdminController;
import com.coniv.mait.web.admin.controller.QuestionStatisticAdminController;

@WebMvcTest(controllers = {
	AdminLookupController.class, AnalyticsAdminController.class, OnboardingAdminController.class,
	OnboardingDevAdminController.class, QuestionStatisticAdminController.class, AnalyticsEventController.class
})
@Import({SecurityConfig.class, JwtAuthorizationFilter.class, JwtTokenProvider.class,
	JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@ActiveProfiles("test")
class AdminApiAuthorizationTest {

	private static final String SCREEN_BODY = """
		{"code":"QUESTION_SOLVE","title":"문제 풀기 가이드"}
		""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private UserEntityRepository userEntityRepository;

	@MockitoBean
	private BlackListRepository blackListRepository;

	@MockitoBean
	private Oauth2UserService oauth2UserService;

	@MockitoBean
	private OAuth2SuccessHandler oauth2SuccessHandler;

	@MockitoBean
	private ClientRegistrationRepository clientRegistrationRepository;

	@MockitoBean
	private IdempotencyInterceptor idempotencyInterceptor;

	@MockitoBean
	private AdminLookupService adminLookupService;

	@MockitoBean
	private AnalyticsEventQueryService analyticsEventQueryService;

	@MockitoBean
	private OnboardingScreenService onboardingScreenService;

	@MockitoBean
	private UserOnboardingService userOnboardingService;

	@MockitoBean
	private QuestionAnswerStatisticService questionAnswerStatisticService;

	@MockitoBean
	private AnalyticsEventCollectService analyticsEventCollectService;

	private UserEntity user;
	private String accessToken;

	@BeforeEach
	void setUp() {
		user = UserEntity.localLoginUser("user@example.com", "password", "사용자", "닉네임");
		ReflectionTestUtils.setField(user, "id", 1L);
		accessToken = jwtTokenProvider.createToken(user.getId()).accessToken();
	}

	@ParameterizedTest
	@MethodSource("adminRequests")
	@DisplayName("익명 사용자는 모든 관리자 조회·변경 API에서 401로 차단한다")
	void anonymousCannotAccessAdminApi(String method, String path) throws Exception {
		// when & then
		mockMvc.perform(adminRequest(method, path))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("A-002"));
		verifyNoAdminServiceCalls();
	}

	@ParameterizedTest
	@MethodSource("adminRequests")
	@DisplayName("로그인한 일반 사용자는 모든 관리자 조회·변경 API에서 403으로 차단한다")
	void userCannotAccessAdminApi(String method, String path) throws Exception {
		// given
		given(userEntityRepository.findById(user.getId())).willReturn(Optional.of(user));

		// when & then
		mockMvc.perform(adminRequest(method, path).header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.code").value("C-008"))
			.andExpect(jsonPath("$.message").value("사용자 권한이 부족합니다."));
		verifyNoAdminServiceCalls();
	}

	@ParameterizedTest
	@ValueSource(strings = {"/api/v1/admin/teams", "/api/v1/admin/teams/10/question-sets",
		"/api/v1/admin/question-sets/20/questions", "/api/v1/admin/analytics/features"})
	@DisplayName("DB에서 관리자 역할을 가진 계정은 관리자 목록을 조회한다")
	void adminCanRead(String path) throws Exception {
		// given
		givenAdmin();

		// when & then
		mockMvc.perform(get(path).header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true));
	}

	@Test
	@DisplayName("관리자는 온보딩 카탈로그를 등록한다")
	void adminCanCreateOnboardingScreen() throws Exception {
		// given
		givenAdmin();
		given(onboardingScreenService.uploadScreen("QUESTION_SOLVE", "문제 풀기 가이드", null))
			.willReturn(OnboardingScreenDto.builder()
				.code("QUESTION_SOLVE").title("문제 풀기 가이드").exposed(true).build());

		// when & then
		mockMvc.perform(post("/api/v1/admin/onboarding/screens")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON).content(SCREEN_BODY))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.code").value("QUESTION_SOLVE"));
		then(onboardingScreenService).should().uploadScreen("QUESTION_SOLVE", "문제 풀기 가이드", null);
	}

	@Test
	@DisplayName("개발 환경의 관리자 초기화 API도 인증한 관리자 계정으로 실행한다")
	void adminCanResetOwnOnboardingHistory() throws Exception {
		// given
		givenAdmin();

		// when & then
		mockMvc.perform(delete("/api/v1/admin/onboarding/views/reset")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk());
		then(userOnboardingService).should().resetViewHistory(user.getId());
	}

	@Test
	@DisplayName("관리자 권한을 회수하면 같은 JWT도 다음 요청부터 차단한다")
	void revokedAdminCannotReuseToken() throws Exception {
		// given
		givenAdmin();
		mockMvc.perform(get("/api/v1/admin/teams").header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk());

		// when
		ReflectionTestUtils.setField(user, "role", UserRole.USER);

		// then
		mockMvc.perform(get("/api/v1/admin/teams").header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isForbidden());
		then(adminLookupService).should(times(1)).getAllTeams();
	}

	@Test
	@DisplayName("일반 사용자는 다른 사용자 ID나 관리자 역할을 요청에 넣어도 접근할 수 없다")
	void userCannotForgeAdminRoleInRequest() throws Exception {
		// given
		given(userEntityRepository.findById(user.getId())).willReturn(Optional.of(user));

		// when & then
		mockMvc.perform(get("/api/v1/admin/teams/999/question-sets")
				.header("Authorization", "Bearer " + accessToken)
				.header("X-User-Role", "ADMIN").param("role", "ADMIN").param("userId", "999"))
			.andExpect(status().isForbidden());
		verifyNoAdminServiceCalls();
	}

	@Test
	@DisplayName("유효하지 않은 JWT로는 관리자 API에 접근할 수 없다")
	void invalidTokenCannotAccessAdminApi() throws Exception {
		// when & then
		mockMvc.perform(get("/api/v1/admin/teams").header("Authorization", "Bearer invalid-token"))
			.andExpect(status().isUnauthorized());
		verifyNoAdminServiceCalls();
	}

	@Test
	@DisplayName("DB에 존재하지 않는 계정의 JWT는 관리자 API에 접근할 수 없다")
	void deletedUserCannotAccessAdminApi() throws Exception {
		// when & then
		mockMvc.perform(get("/api/v1/admin/teams").header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isUnauthorized());
		verifyNoAdminServiceCalls();
	}

	@Test
	@DisplayName("일반 사용자의 분석 이벤트 수집은 기존처럼 로그인만 요구한다")
	void userCanCollectAnalyticsEvents() throws Exception {
		// given
		given(userEntityRepository.findById(user.getId())).willReturn(Optional.of(user));
		String body = """
			{"featureKey":"onboarding","eventName":"screen_view","sessionId":"session-1","step":0}
			""";

		// when & then
		mockMvc.perform(post("/api/v1/analytics/events").header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isOk());
		then(analyticsEventCollectService).should()
			.collect(user.getId(), "onboarding", "screen_view", "session-1", 0, null);
	}

	@Test
	@DisplayName("소셜 가입자도 기본 일반 사용자 역할로 생성되어 관리자 접근이 거절된다")
	void socialUserCannotAccessAdminApi() throws Exception {
		// given
		UserEntity socialUser = UserEntity.socialLoginUser("social@example.com", "사용자", "google", LoginProvider.GOOGLE);
		ReflectionTestUtils.setField(socialUser, "id", user.getId());
		given(userEntityRepository.findById(user.getId())).willReturn(Optional.of(socialUser));

		// when & then
		mockMvc.perform(get("/api/v1/admin/teams").header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isForbidden());
		verifyNoAdminServiceCalls();
	}

	private void givenAdmin() {
		ReflectionTestUtils.setField(user, "role", UserRole.ADMIN);
		given(userEntityRepository.findById(user.getId())).willReturn(Optional.of(user));
		given(adminLookupService.getAllTeams()).willReturn(List.of());
	}

	private void verifyNoAdminServiceCalls() {
		then(adminLookupService).shouldHaveNoInteractions();
		then(analyticsEventQueryService).shouldHaveNoInteractions();
		then(onboardingScreenService).shouldHaveNoInteractions();
		then(userOnboardingService).shouldHaveNoInteractions();
		then(questionAnswerStatisticService).shouldHaveNoInteractions();
	}

	private MockHttpServletRequestBuilder adminRequest(String method, String path) {
		return request(HttpMethod.valueOf(method), path).contentType(MediaType.APPLICATION_JSON).content(SCREEN_BODY);
	}

	private static Stream<Arguments> adminRequests() {
		return Stream.of(
			Arguments.of("GET", "/api/v1/admin/teams"),
			Arguments.of("GET", "/api/v1/admin/teams/10/question-sets"),
			Arguments.of("GET", "/api/v1/admin/question-sets/20/questions"),
			Arguments.of("GET", "/api/v1/admin/question-sets/20/questions/30/statistics/answer-distribution"),
			Arguments.of("GET", "/api/v1/admin/analytics/features"),
			Arguments.of("GET", "/api/v1/admin/analytics/features/40/event-stats"),
			Arguments.of("POST", "/api/v1/admin/onboarding/screens"),
			Arguments.of("DELETE", "/api/v1/admin/onboarding/views/reset"),
			Arguments.of("PATCH", "/api/v1/admin/future-resource/1"));
	}
}
