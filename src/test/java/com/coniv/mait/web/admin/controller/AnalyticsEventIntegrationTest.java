package com.coniv.mait.web.admin.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.coniv.mait.domain.admin.entity.AnalyticsEventEntity;
import com.coniv.mait.domain.admin.entity.AnalyticsFeatureEntity;
import com.coniv.mait.domain.admin.repository.AnalyticsEventRepository;
import com.coniv.mait.domain.admin.repository.AnalyticsFeatureRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@WithCustomUser
public class AnalyticsEventIntegrationTest extends BaseIntegrationTest {

	private static final String SESSION_ID = "550e8400-e29b-41d4-a716-446655440000";

	@Autowired
	private AnalyticsEventRepository analyticsEventRepository;

	@Autowired
	private AnalyticsFeatureRepository analyticsFeatureRepository;

	@Autowired
	private UserEntityRepository userEntityRepository;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@BeforeEach
	void setUp() throws Exception {
		analyticsEventRepository.deleteAll();
		analyticsFeatureRepository.deleteAll();
		analyticsFeatureRepository.save(AnalyticsFeatureEntity.of("onboarding"));
		Mockito.doAnswer(inv -> {
			ServletRequest request = inv.getArgument(0);
			ServletResponse response = inv.getArgument(1);
			FilterChain chain = inv.getArgument(2);
			chain.doFilter(request, response);
			return null;
		}).when(jwtAuthorizationFilter).doFilter(Mockito.any(), Mockito.any(), Mockito.any());
	}

	@Test
	@DisplayName("분석 이벤트를 수집하면 인증 사용자 기준으로 DB 에 저장된다")
	void collect_persistsEvent() throws Exception {
		// given
		UserEntity user = userEntityRepository.findByEmail("user@example.com").orElseThrow();

		// when & then
		mockMvc.perform(post("/api/v1/analytics/events")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"featureKey": "onboarding",
						"eventName": "player_set_list_view",
						"sessionId": "%s",
						"step": 2
					}
					""".formatted(SESSION_ID)))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data").doesNotExist());

		List<AnalyticsEventEntity> events = analyticsEventRepository.findAll();
		assertThat(events).hasSize(1);
		AnalyticsEventEntity saved = events.get(0);
		assertThat(saved.getUserId()).isEqualTo(user.getId());
		assertThat(saved.getFeature().getFeatureKey()).isEqualTo("onboarding");
		assertThat(saved.getEventName()).isEqualTo("player_set_list_view");
		assertThat(saved.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(saved.getStep()).isEqualTo(2);
		assertThat(saved.getOccurredAt()).isNotNull();
	}

	@Test
	@DisplayName("동일 조합의 이벤트를 중복 전송해도 1건만 저장된다")
	void collect_ignoresDuplicate() throws Exception {
		// given
		String body = """
			{
				"featureKey": "onboarding",
				"eventName": "player_set_list_view",
				"sessionId": "%s",
				"step": 2
			}
			""".formatted(SESSION_ID);

		// when
		for (int i = 0; i < 2; i++) {
			mockMvc.perform(post("/api/v1/analytics/events")
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andExpect(status().isOk());
		}

		// then
		assertThat(analyticsEventRepository.findAll()).hasSize(1);
	}
}
