package com.coniv.mait.web.onboarding.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
		given(userOnboardingService.getViewStatus(anyLong(), eq(OnboardingScreenCode.HOME_GUIDE)))
			.willReturn(new OnboardingViewStatusDto(true, true));

		// when & then
		mockMvc.perform(get("/api/v1/onboarding/screens/view-status")
				.param("code", "HOME_GUIDE"))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.viewed").value(true),
				jsonPath("$.data.dismissed").value(true));
	}
}
