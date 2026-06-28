package com.coniv.mait.web.onboarding.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.coniv.mait.domain.onboarding.enums.OnboardingScreenCode;
import com.coniv.mait.domain.onboarding.service.UserOnboardingService;
import com.coniv.mait.domain.onboarding.service.dto.OnboardingScreenDto;
import com.coniv.mait.domain.onboarding.service.dto.OnboardingViewStatusDto;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@WithCustomUser
public class OnboardingControllerTest extends BaseIntegrationTest {

	@MockitoBean
	private UserOnboardingService userOnboardingService;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@BeforeEach
	void passThroughJwtFilter() throws Exception {
		Mockito.doAnswer(inv -> {
			ServletRequest request = inv.getArgument(0);
			ServletResponse response = inv.getArgument(1);
			FilterChain chain = inv.getArgument(2);
			chain.doFilter(request, response);
			return null;
		}).when(jwtAuthorizationFilter).doFilter(Mockito.any(), Mockito.any(), Mockito.any());
	}

	@Test
	@DisplayName("미열람 온보딩 화면 목록 조회에 성공한다")
	void getUnviewedScreens_success() throws Exception {
		// given
		given(userOnboardingService.getUnviewedScreens(any())).willReturn(List.of(
			OnboardingScreenDto.builder()
				.id(1L)
				.code(OnboardingScreenCode.QUESTION_SOLVE)
				.title("문제 풀기 가이드")
				.exposed(true)
				.targetTeamRole(null)
				.build()));

		// when & then
		mockMvc.perform(get("/api/v1/onboarding/screens/unviewed"))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.length()").value(1),
				jsonPath("$.data[0].id").value(1),
				jsonPath("$.data[0].code").value("QUESTION_SOLVE"),
				jsonPath("$.data[0].title").value("문제 풀기 가이드"));
	}

	@Test
	@DisplayName("특정 온보딩 화면 열람 상태 조회에 성공한다")
	void getViewStatus_success() throws Exception {
		// given
		given(userOnboardingService.getViewStatus(anyLong(), eq(1L)))
			.willReturn(new OnboardingViewStatusDto(true, true));

		// when & then
		mockMvc.perform(get("/api/v1/onboarding/screens/1/view-status"))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.viewed").value(true),
				jsonPath("$.data.dismissed").value(true));
	}

	@Test
	@DisplayName("특정 온보딩 화면 열람 기록에 성공한다")
	void recordView_success() throws Exception {
		// when & then
		mockMvc.perform(post("/api/v1/onboarding/screens/view")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"screenId": 1,
						"dismissed": true
					}
					"""))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data").doesNotExist());

		then(userOnboardingService).should().recordView(anyLong(), eq(1L), eq(true));
	}

	@ParameterizedTest
	@MethodSource("recordViewValidationFailureCases")
	@DisplayName("특정 온보딩 화면 열람 기록 요청값이 유효하지 않으면 400 을 응답한다")
	void recordView_validationFail(String body, String expectedReason) throws Exception {
		// when & then
		mockMvc.perform(post("/api/v1/onboarding/screens/view")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpectAll(
				status().isBadRequest(),
				jsonPath("$.isSuccess").value(false),
				jsonPath("$.code").value("C-001"),
				jsonPath("$.message").value("사용자 입력 오류입니다."),
				jsonPath("$.reasons[0]").value(expectedReason));

		then(userOnboardingService).should(never()).recordView(anyLong(), any(), anyBoolean());
	}

	private static Stream<Arguments> recordViewValidationFailureCases() {
		return Stream.of(
			Arguments.of("""
				{
					"dismissed": true
				}
				""", "온보딩 화면 ID는 필수입니다."),
			Arguments.of("""
				{
					"screenId": 1
				}
				""", "다시 보지 않기 선택 여부는 필수입니다.")
		);
	}
}
