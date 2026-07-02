package com.coniv.mait.web.admin.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

import com.coniv.mait.domain.admin.service.AnalyticsEventCollectService;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@WithCustomUser
public class AnalyticsEventControllerTest extends BaseIntegrationTest {

	@MockitoBean
	private AnalyticsEventCollectService analyticsEventCollectService;

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
	@DisplayName("분석 이벤트 수집에 성공한다")
	void collect_success() throws Exception {
		// when & then
		mockMvc.perform(post("/api/v1/analytics/events")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"featureKey": "onboarding",
						"eventName": "player_set_list_view",
						"sessionId": "550e8400-e29b-41d4-a716-446655440000",
						"step": 2,
						"metadata": "context"
					}
					"""))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data").doesNotExist());

		then(analyticsEventCollectService).should().collect(anyLong(), eq("onboarding"),
			eq("player_set_list_view"), eq("550e8400-e29b-41d4-a716-446655440000"), eq(2), eq("context"));
	}

	@ParameterizedTest
	@MethodSource("collectValidationFailureCases")
	@DisplayName("분석 이벤트 수집 요청값이 유효하지 않으면 400 을 응답한다")
	void collect_validationFail(String body, String expectedReason) throws Exception {
		// when & then
		mockMvc.perform(post("/api/v1/analytics/events")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpectAll(
				status().isBadRequest(),
				jsonPath("$.isSuccess").value(false),
				jsonPath("$.code").value("C-001"),
				jsonPath("$.message").value("사용자 입력 오류입니다."),
				jsonPath("$.reasons[0]").value(expectedReason));

		then(analyticsEventCollectService).should(never()).collect(any(), any(), any(), any(), any(), any());
	}

	private static Stream<Arguments> collectValidationFailureCases() {
		return Stream.of(
			Arguments.of("""
				{
					"eventName": "player_set_list_view",
					"sessionId": "550e8400-e29b-41d4-a716-446655440000"
				}
				""", "기능 그룹은 필수입니다."),
			Arguments.of("""
				{
					"featureKey": "onboarding",
					"sessionId": "550e8400-e29b-41d4-a716-446655440000"
				}
				""", "이벤트명은 필수입니다."),
			Arguments.of("""
				{
					"featureKey": "onboarding",
					"eventName": "player_set_list_view"
				}
				""", "세션 ID는 필수입니다."),
			Arguments.of("""
				{
					"featureKey": "onboarding",
					"eventName": "player_set_list_view",
					"sessionId": "550e8400-e29b-41d4-a716-446655440000",
					"step": -1
				}
				""", "단계는 0 이상이어야 합니다.")
		);
	}
}
