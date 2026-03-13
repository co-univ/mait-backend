package com.coniv.mait.web.question.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.coniv.mait.domain.question.service.TeamQuestionRankService;
import com.coniv.mait.domain.question.service.dto.PersonalAccuracyDto;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;

@WebMvcTest(controllers = TeamQuestionRankController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeamQuestionRankControllerTest {

	private static final Long USER_ID = 1L;
	private static final Long TEAM_ID = 1L;

	@MockitoBean
	private TeamQuestionRankService teamQuestionRankService;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private IdempotencyInterceptor idempotencyInterceptor;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@BeforeEach
	void setUp() throws Exception {
		when(idempotencyInterceptor.preHandle(any(), any(), any())).thenReturn(true);

		MaitUser user = MaitUser.builder().id(USER_ID).build();
		var authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());
		var context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("개인 정답률 조회 API 성공 테스트")
	void getPersonalAccuracy_Success() throws Exception {
		// given
		PersonalAccuracyDto dto = PersonalAccuracyDto.of(10, 7);

		when(teamQuestionRankService.getPersonalAccuracy(TEAM_ID, USER_ID)).thenReturn(dto);

		// when & then
		mockMvc.perform(get("/api/v1/teams/{teamId}/user-solving-stats", TEAM_ID))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.totalSolvedCount").value(10),
				jsonPath("$.data.correctCount").value(7),
				jsonPath("$.data.accuracyRate").value(70.0)
			);

		verify(teamQuestionRankService).getPersonalAccuracy(TEAM_ID, USER_ID);
	}

	@Test
	@DisplayName("개인 정답률 조회 API - 풀이 기록이 없는 경우")
	void getPersonalAccuracy_NoRecords() throws Exception {
		// given
		PersonalAccuracyDto dto = PersonalAccuracyDto.of(0, 0);

		when(teamQuestionRankService.getPersonalAccuracy(TEAM_ID, USER_ID)).thenReturn(dto);

		// when & then
		mockMvc.perform(get("/api/v1/teams/{teamId}/user-solving-stats", TEAM_ID))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.totalSolvedCount").value(0),
				jsonPath("$.data.correctCount").value(0),
				jsonPath("$.data.accuracyRate").value(0.0)
			);
	}
}
