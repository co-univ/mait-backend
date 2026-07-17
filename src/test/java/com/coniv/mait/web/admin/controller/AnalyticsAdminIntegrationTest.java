package com.coniv.mait.web.admin.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.coniv.mait.domain.admin.entity.AnalyticsEventEntity;
import com.coniv.mait.domain.admin.entity.AnalyticsFeatureEntity;
import com.coniv.mait.domain.admin.repository.AnalyticsEventRepository;
import com.coniv.mait.domain.admin.repository.AnalyticsFeatureRepository;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@WithCustomUser
public class AnalyticsAdminIntegrationTest extends BaseIntegrationTest {

	private static final String SESSION_A = "550e8400-e29b-41d4-a716-446655440000";
	private static final String SESSION_B = "550e8400-e29b-41d4-a716-446655440001";

	@Autowired
	private AnalyticsEventRepository analyticsEventRepository;

	@Autowired
	private AnalyticsFeatureRepository analyticsFeatureRepository;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	private AnalyticsFeatureEntity onboarding;

	@BeforeEach
	void setUp() throws Exception {
		analyticsEventRepository.deleteAll();
		analyticsFeatureRepository.deleteAll();
		onboarding = analyticsFeatureRepository.save(AnalyticsFeatureEntity.of("onboarding"));

		saveEvent("player_set_list_view", 1L, SESSION_A, 0);
		saveEvent("player_set_list_view", 2L, SESSION_B, 0);
		saveEvent("player_set_list_exit", 1L, SESSION_A, 3);
		saveEvent("player_set_list_exit", 2L, SESSION_B, 1);

		Mockito.doAnswer(inv -> {
			ServletRequest request = inv.getArgument(0);
			ServletResponse response = inv.getArgument(1);
			FilterChain chain = inv.getArgument(2);
			chain.doFilter(request, response);
			return null;
		}).when(jwtAuthorizationFilter).doFilter(Mockito.any(), Mockito.any(), Mockito.any());
	}

	@Test
	@DisplayName("등록된 feature 목록을 조회한다")
	void getFeatures_returnsRegisteredFeatures() throws Exception {
		mockMvc.perform(get("/api/v1/admin/analytics/features"))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.length()").value(1),
				jsonPath("$.data[0].id").value(onboarding.getId()),
				jsonPath("$.data[0].featureKey").value("onboarding"));
	}

	@Test
	@DisplayName("feature에 저장된 이벤트를 (event_name, step) 단위로 집계해 응답한다")
	void getEventStats_aggregatesPersistedEvents() throws Exception {
		mockMvc.perform(get("/api/v1/admin/analytics/features/{featureId}/event-stats", onboarding.getId()))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.featureKey").value("onboarding"),
				jsonPath("$.data.totalCount").value(4),
				jsonPath("$.data.events[0].eventName").value("player_set_list_exit"),
				jsonPath("$.data.events[0].count").value(2),
				jsonPath("$.data.events[0].steps[0].step").value(1),
				jsonPath("$.data.events[0].steps[0].count").value(1),
				jsonPath("$.data.events[0].steps[1].step").value(3),
				jsonPath("$.data.events[0].steps[1].count").value(1),
				jsonPath("$.data.events[1].eventName").value("player_set_list_view"),
				jsonPath("$.data.events[1].count").value(2),
				jsonPath("$.data.events[1].steps[0].step").value(0),
				jsonPath("$.data.events[1].steps[0].count").value(2));
	}

	private void saveEvent(final String eventName, final Long userId, final String sessionId, final int step) {
		analyticsEventRepository.save(AnalyticsEventEntity.builder()
			.feature(onboarding)
			.eventName(eventName)
			.userId(userId)
			.sessionId(sessionId)
			.step(step)
			.occurredAt(LocalDateTime.now())
			.build());
	}
}
