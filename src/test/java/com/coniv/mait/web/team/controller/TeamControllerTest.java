package com.coniv.mait.web.team.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.coniv.mait.domain.team.service.TeamService;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.global.enums.InviteTokenDuration;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;
import com.coniv.mait.web.team.dto.CreateTeamApiRequest;
import com.coniv.mait.web.team.dto.CreateTeamInviteApiRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = TeamController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeamControllerTest {

	@MockitoBean
	private TeamService teamService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private IdempotencyInterceptor idempotencyInterceptor;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@Test
	@DisplayName("팀 생성 API 성공 테스트")
	void createTeam_Success() throws Exception {
		// given
		CreateTeamApiRequest request = new CreateTeamApiRequest("테스트 팀");

		doNothing().when(teamService).createTeam(eq("테스트 팀"), nullable(UserEntity.class));

		// when & then
		mockMvc.perform(post("/api/v1/teams")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").doesNotExist());

		verify(teamService).createTeam(eq("테스트 팀"), nullable(UserEntity.class));
	}

	@Test
	@DisplayName("팀 생성 API 실패 테스트 - 유효하지 않은 팀 이름")
	void createTeam_Failure_InvalidTeamName() throws Exception {
		// given
		CreateTeamApiRequest request = new CreateTeamApiRequest("");

		// when & then
		mockMvc.perform(post("/api/v1/teams")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());

		verify(teamService, never()).createTeam(anyString(), any(UserEntity.class));
	}

	@Test
	@DisplayName("팀 초대 코드 생성 API 성공 테스트")
	void createTeamInviteCode_Success() throws Exception {
		// given
		Long teamId = 1L;
		CreateTeamInviteApiRequest request = new CreateTeamInviteApiRequest(InviteTokenDuration.ONE_DAY);
		String expectedInviteCode = "INVITE123";

		when(teamService.createTeamInviteCode(eq(teamId), nullable(UserEntity.class), eq(InviteTokenDuration.ONE_DAY)))
			.thenReturn(expectedInviteCode);

		// when & then

		mockMvc.perform(post("/api/v1/teams/{teamId}/invite", teamId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").exists(),
				jsonPath("$.data.token").value(expectedInviteCode)
			);

		verify(teamService).createTeamInviteCode(eq(teamId), nullable(UserEntity.class),
			eq(InviteTokenDuration.ONE_DAY));
	}
}
