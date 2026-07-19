package com.coniv.mait.web.admin.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.coniv.mait.domain.admin.entity.AnalyticsFeatureEntity;
import com.coniv.mait.domain.admin.service.AnalyticsEventQueryService;
import com.coniv.mait.domain.admin.service.dto.AnalyticsEventStatsDto;
import com.coniv.mait.domain.admin.service.dto.AnalyticsEventStepCountDto;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@WithCustomUser
public class AnalyticsAdminControllerTest extends BaseIntegrationTest {

	@MockitoBean
	private AnalyticsEventQueryService analyticsEventQueryService;

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
	@DisplayName("분석 feature 목록 조회에 성공한다")
	void getFeatures_success() throws Exception {
		// given
		BDDMockito.given(analyticsEventQueryService.getFeatures()).willReturn(List.of(
			AnalyticsFeatureEntity.builder().id(1L).featureKey("onboarding").build()));

		// when & then
		mockMvc.perform(get("/api/v1/admin/analytics/features"))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data[0].id").value(1),
				jsonPath("$.data[0].featureKey").value("onboarding"));
	}

	@Test
	@DisplayName("feature별 이벤트 통계 조회에 성공한다")
	void getEventStats_success() throws Exception {
		// given
		BDDMockito.given(analyticsEventQueryService.getEventStats(1L)).willReturn(
			new AnalyticsEventStatsDto("onboarding", List.of(
				new AnalyticsEventStepCountDto("player_set_list_exit", 1, 1),
				new AnalyticsEventStepCountDto("player_set_list_exit", 3, 2),
				new AnalyticsEventStepCountDto("player_set_list_view", 0, 2))));

		// when & then
		mockMvc.perform(get("/api/v1/admin/analytics/features/1/event-stats"))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.featureKey").value("onboarding"),
				jsonPath("$.data.totalCount").value(5),
				jsonPath("$.data.events[0].eventName").value("player_set_list_exit"),
				jsonPath("$.data.events[0].count").value(3),
				jsonPath("$.data.events[0].steps[0].step").value(1),
				jsonPath("$.data.events[0].steps[0].count").value(1),
				jsonPath("$.data.events[0].steps[1].step").value(3),
				jsonPath("$.data.events[0].steps[1].count").value(2),
				jsonPath("$.data.events[1].eventName").value("player_set_list_view"),
				jsonPath("$.data.events[1].count").value(2),
				jsonPath("$.data.events[1].steps[0].step").value(0),
				jsonPath("$.data.events[1].steps[0].count").value(2));
	}
}
