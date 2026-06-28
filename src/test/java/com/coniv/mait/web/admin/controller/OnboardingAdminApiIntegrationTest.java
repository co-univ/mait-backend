package com.coniv.mait.web.admin.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.coniv.mait.domain.onboarding.entity.OnboardingScreenEntity;
import com.coniv.mait.domain.onboarding.repository.OnboardingScreenRepository;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.web.integration.BaseIntegrationTest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

public class OnboardingAdminApiIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private OnboardingScreenRepository onboardingScreenRepository;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@BeforeEach
	void setUp() throws Exception {
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
	@DisplayName("온보딩 화면을 신규 등록하면 기본 노출(true) 상태로 저장된다")
	void uploadScreen_create_success() throws Exception {
		// given
		String body = """
			{"code":"QUESTION_SOLVE","title":"문제 풀기 가이드","targetTeamRole":"MAKER"}
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
				jsonPath("$.data.exposed").value(true),
				jsonPath("$.data.targetTeamRole").value("MAKER"));

		OnboardingScreenEntity saved = onboardingScreenRepository.findAll().get(0);
		assertThat(saved.getTitle()).isEqualTo("문제 풀기 가이드");
		assertThat(saved.isExposed()).isTrue();
		assertThat(saved.getTargetTeamRole()).isEqualTo(TeamUserRole.MAKER);
	}
}
