package com.coniv.mait.web.admin.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.coniv.mait.domain.onboarding.service.OnboardingScreenService;
import com.coniv.mait.domain.onboarding.service.dto.OnboardingScreenDto;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.web.integration.BaseIntegrationTest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

public class OnboardingAdminControllerTest extends BaseIntegrationTest {

	@MockitoBean
	private OnboardingScreenService onboardingScreenService;

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
	@DisplayName("온보딩 화면 등록에 성공한다")
	void uploadScreen_success() throws Exception {
		// given
		given(onboardingScreenService.uploadScreen(any(), any(), any())).willReturn(
			OnboardingScreenDto.builder()
				.id(1L)
				.code("QUESTION_SOLVE")
				.title("문제 풀기 가이드")
				.exposed(true)
				.targetTeamRole(null)
				.createdAt(LocalDateTime.now())
				.modifiedAt(LocalDateTime.now())
				.build());

		String body = """
			{"code":"QUESTION_SOLVE","title":"문제 풀기 가이드","targetTeamRole":null}
			""";

		// when & then
		mockMvc.perform(post("/api/v1/admin/onboarding/screens")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.code").value("QUESTION_SOLVE"),
				jsonPath("$.data.title").value("문제 풀기 가이드"),
				jsonPath("$.data.exposed").value(true));
	}

	@Test
	@DisplayName("code 가 없으면 400 을 응답한다")
	void uploadScreen_codeNull_badRequest() throws Exception {
		// given
		String body = """
			{"title":"문제 풀기 가이드"}
			""";

		// when & then
		mockMvc.perform(post("/api/v1/admin/onboarding/screens")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("title 이 비어있으면 400 을 응답한다")
	void uploadScreen_titleBlank_badRequest() throws Exception {
		// given
		String body = """
			{"code":"QUESTION_SOLVE","title":""}
			""";

		// when & then
		mockMvc.perform(post("/api/v1/admin/onboarding/screens")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isBadRequest());
	}
}
