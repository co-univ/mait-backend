package com.coniv.mait.web.team.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.coniv.mait.domain.team.enums.TeamType;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.service.TeamService;
import com.coniv.mait.domain.team.service.dto.TeamInvitationDto;
import com.coniv.mait.domain.team.service.dto.TeamUserDto;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.global.enums.InviteTokenDuration;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;
import com.coniv.mait.web.team.dto.CreateTeamApiRequest;
import com.coniv.mait.web.team.dto.CreateTeamInviteApiRequest;
import com.coniv.mait.web.team.dto.UpdateTeamNameApiRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = TeamController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeamControllerTest {

	private static final Long USER_ID = 1L;

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
	@DisplayName("팀 생성 API 성공 테스트")
	void createTeam_Success() throws Exception {
		// given
		CreateTeamApiRequest request = new CreateTeamApiRequest("테스트 팀");

		doNothing().when(teamService).createTeam(eq("테스트 팀"), eq(USER_ID));

		// when & then
		mockMvc.perform(post("/api/v1/teams")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").doesNotExist());

		verify(teamService).createTeam(eq("테스트 팀"), eq(USER_ID));
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

		verify(teamService, never()).createTeam(anyString(), any(Long.class));
	}

	@Test
	@DisplayName("팀 이름 변경 API 성공 테스트")
	void updateTeamName_Success() throws Exception {
		// given
		Long teamId = 1L;
		UpdateTeamNameApiRequest request = new UpdateTeamNameApiRequest("변경된 팀");

		doNothing().when(teamService).updateTeamName(eq(teamId), eq("변경된 팀"), eq(USER_ID));

		// when & then
		mockMvc.perform(patch("/api/v1/teams/{teamId}/name", teamId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").doesNotExist());

		verify(teamService).updateTeamName(eq(teamId), eq("변경된 팀"), eq(USER_ID));
	}

	@Test
	@DisplayName("팀 이름 변경 API 실패 테스트 - 유효하지 않은 팀 이름")
	void updateTeamName_Failure_InvalidTeamName() throws Exception {
		// given
		Long teamId = 1L;
		UpdateTeamNameApiRequest request = new UpdateTeamNameApiRequest("");

		// when & then
		mockMvc.perform(patch("/api/v1/teams/{teamId}/name", teamId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());

		verify(teamService, never()).updateTeamName(any(Long.class), anyString(), any(Long.class));
	}

	@Test
	@DisplayName("팀 초대 코드 생성 API 성공 테스트")
	void createTeamInviteCode_Success() throws Exception {
		// given
		Long teamId = 1L;
		TeamUserRole role = TeamUserRole.PLAYER;
		boolean requiresApproval = true;
		CreateTeamInviteApiRequest request = new CreateTeamInviteApiRequest(InviteTokenDuration.ONE_DAY, role);
		String expectedInviteCode = "INVITE123";

		when(teamService.createTeamInviteCode(eq(teamId), eq(USER_ID),
			eq(InviteTokenDuration.ONE_DAY), eq(role), eq(requiresApproval)))
			.thenReturn(expectedInviteCode);

		// when & then
		mockMvc.perform(post("/api/v1/teams/{teamId}/invitation", teamId)
				.param("requiresApproval", String.valueOf(requiresApproval))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").exists(),
				jsonPath("$.data.token").value(expectedInviteCode)
			);

		verify(teamService).createTeamInviteCode(eq(teamId), eq(USER_ID),
			eq(InviteTokenDuration.ONE_DAY), eq(role), eq(requiresApproval));
	}

	@Test
	@DisplayName("팀 초대 정보 조회 API 성공 테스트")
	void getTeamInfo_Success() throws Exception {
		// given
		String code = "INV123";
		TeamInvitationDto dto = TeamInvitationDto.builder()
			.teamInviteId(1L)
			.teamId(2L)
			.invitorId(3L)
			.teamName("테스트팀")
			.tokenDuration(InviteTokenDuration.ONE_DAY)
			.requiresApproval(false)
			.teamUserRole(TeamUserRole.PLAYER)
			.expiredAt(LocalDateTime.now().plusDays(1))
			.build();

		when(teamService.getTeamInviteInfo(any(MaitUser.class), eq(code))).thenReturn(dto);

		// when & then
		mockMvc.perform(get("/api/v1/teams/invitation/info").param("code", code)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").exists())
			.andExpect(jsonPath("$.data.teamId").value(dto.getTeamId()))
			.andExpect(jsonPath("$.data.teamName").value(dto.getTeamName()))
			.andExpect(jsonPath("$.data.role").value(dto.getTeamUserRole().name()))
			.andExpect(jsonPath("$.data.requiresApproval").value(dto.isRequiresApproval()));

		verify(teamService).getTeamInviteInfo(any(MaitUser.class), eq(code));
	}

	@Test
	@DisplayName("팀 탈퇴 API 성공 테스트")
	void leaveTeam_Success() throws Exception {
		// given
		Long teamId = 1L;

		doNothing().when(teamService).leaveTeam(eq(teamId), eq(USER_ID));

		// when & then
		mockMvc.perform(delete("/api/v1/teams/{teamId}/users/me", teamId))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data").doesNotExist()
			);

		verify(teamService).leaveTeam(eq(teamId), eq(USER_ID));
	}

	@Test
	@DisplayName("팀 삭제 API 성공 테스트")
	void deleteTeam_Success() throws Exception {
		// given
		Long teamId = 1L;

		doNothing().when(teamService).deleteTeam(eq(teamId), eq(USER_ID));

		// when & then
		mockMvc.perform(delete("/api/v1/teams/{teamId}", teamId))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data").doesNotExist()
			);

		verify(teamService).deleteTeam(eq(teamId), eq(USER_ID));
	}

	@Test
	@DisplayName("가입 팀 목록 조회 API 성공 테스트 - role 미지정 시 서비스에 null 을 전달한다")
	void getJoinedTeams_Success_WithoutRole() throws Exception {
		// given
		doReturn(List.of(joinedTeam("오너 팀", TeamUserRole.OWNER)))
			.when(teamService).getJoinedTeams(USER_ID, null);

		// when & then
		mockMvc.perform(get("/api/v1/teams/joined"))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data[0].teamName").value("오너 팀"),
				jsonPath("$.data[0].role").value("OWNER")
			);

		verify(teamService).getJoinedTeams(USER_ID, null);
	}

	@Test
	@DisplayName("가입 팀 목록 조회 API 성공 테스트 - role 파라미터가 서비스로 그대로 바인딩된다")
	void getJoinedTeams_Success_WithRole() throws Exception {
		// given
		doReturn(List.of(joinedTeam("메이커 팀", TeamUserRole.MAKER)))
			.when(teamService).getJoinedTeams(USER_ID, TeamUserRole.MAKER);

		// when & then
		mockMvc.perform(get("/api/v1/teams/joined").param("role", "MAKER"))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.data[0].role").value("MAKER")
			);

		verify(teamService).getJoinedTeams(USER_ID, TeamUserRole.MAKER);
	}

	@Test
	@DisplayName("가입 팀 목록 조회 API 실패 테스트 - 정의되지 않은 role 값은 400 을 반환한다")
	void getJoinedTeams_Failure_InvalidRole() throws Exception {
		// when & then
		mockMvc.perform(get("/api/v1/teams/joined").param("role", "INVALID"))
			.andExpect(status().isBadRequest());

		verify(teamService, never()).getJoinedTeams(anyLong(), any());
	}

	private TeamUserDto joinedTeam(final String teamName, final TeamUserRole role) {
		return TeamUserDto.builder()
			.teamId(1L)
			.teamName(teamName)
			.teamType(TeamType.GROUP)
			.role(role)
			.build();
	}
}
