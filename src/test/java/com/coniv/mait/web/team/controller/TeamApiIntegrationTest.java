package com.coniv.mait.web.team.controller;

import static com.coniv.mait.domain.user.enums.LoginProvider.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamInviteEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamInviteEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.enums.InviteTokenDuration;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;
import com.coniv.mait.web.team.dto.CreateTeamApiRequest;
import com.coniv.mait.web.team.dto.CreateTeamInviteApiRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

public class TeamApiIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	JwtAuthorizationFilter jwtAuthenticationFilter;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private TeamEntityRepository teamEntityRepository;

	@Autowired
	private TeamUserEntityRepository teamUserEntityRepository;

	@Autowired
	private TeamInviteEntityRepository teamInviteEntityRepository;

	@AfterEach
	void clear() {
		teamInviteEntityRepository.deleteAll();
		teamUserEntityRepository.deleteAll();
		teamEntityRepository.deleteAll();
		userEntityRepository.deleteAll();
	}

	@BeforeEach
	void passThroughJwtFilter() throws Exception {
		Mockito.doAnswer(inv -> {
			var request = (ServletRequest)inv.getArgument(0);
			var response = (ServletResponse)inv.getArgument(1);
			var chain = (FilterChain)inv.getArgument(2);
			chain.doFilter(request, response);
			return null;
		}).when(jwtAuthenticationFilter).doFilter(Mockito.any(), Mockito.any(), Mockito.any());
	}

	@Test
	@Transactional
	@WithCustomUser(email = "test@example.com", name = "사용자1")
	@DisplayName("팀 생성 API 통합 테스트 - 성공")
	void createTeam_Success() throws Exception {
		// given
		CreateTeamApiRequest request = new CreateTeamApiRequest("테스트 팀");

		System.out.println(userEntityRepository.findAll().size() + " size는 ");

		// when & then
		mockMvc.perform(post("/api/v1/teams")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf())
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data").doesNotExist());

		// then
		List<TeamEntity> teams = teamEntityRepository.findAll();
		List<TeamUserEntity> teamUsers = teamUserEntityRepository.findAll();

		assertThat(teams).hasSize(1);
		TeamEntity savedTeam = teams.get(0);
		assertThat(savedTeam.getName()).isEqualTo("테스트 팀");

		// 어노테이션로 저장된 사용자 조회
		UserEntity user = userEntityRepository.findByEmail("test@example.com").orElseThrow();

		assertThat(savedTeam.getCreatorId()).isEqualTo(user.getId());

		assertThat(teamUsers).hasSize(1);
		TeamUserEntity savedTeamUser = teamUsers.get(0);
		assertThat(savedTeamUser.getUser().getId()).isEqualTo(user.getId());
		assertThat(savedTeamUser.getTeam().getId()).isEqualTo(savedTeam.getId());
		assertThat(savedTeamUser.getUserRole()).isEqualTo(TeamUserRole.OWNER);
	}

	@Test
	@WithMockUser
	@DisplayName("팀 생성 API 통합 테스트 - 실패: 빈 팀 이름")
	void createTeam_Failure_EmptyName() throws Exception {
		// given
		UserEntity user = createTestUser("test@example.com", "테스트 사용자");
		CreateTeamApiRequest request = new CreateTeamApiRequest("");

		// when & then
		mockMvc.perform(post("/api/v1/teams")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());

		// then
		assertThat(teamEntityRepository.findAll()).isEmpty();
		assertThat(teamUserEntityRepository.findAll()).isEmpty();
	}

	@Test
	@WithMockUser
	@DisplayName("팀 생성 API 통합 테스트 - 실패: null 팀 이름")
	void createTeam_Failure_NullName() throws Exception {
		// given
		UserEntity user = createTestUser("test@example.com", "테스트 사용자");
		String requestJson = "{\"name\": null}";

		// when & then
		mockMvc.perform(post("/api/v1/teams")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson))
			.andExpect(status().isBadRequest());

		// then
		assertThat(teamEntityRepository.findAll()).isEmpty();
		assertThat(teamUserEntityRepository.findAll()).isEmpty();
	}

	@Test
	@WithMockUser
	@DisplayName("팀 초대 코드 생성 API 통합 테스트 - 성공")
	void createTeamInviteCode_Success() throws Exception {
		// given
		UserEntity user = createTestUser("test@example.com", "테스트 사용자");
		TeamEntity team = createTeamWithOwner("테스트 팀", user);
		CreateTeamInviteApiRequest request = new CreateTeamInviteApiRequest(InviteTokenDuration.ONE_DAY);

		// when & then
		mockMvc.perform(post("/api/v1/teams/{teamId}/invite", team.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.inviteCode").exists())
			.andExpect(jsonPath("$.data.inviteCode").isString());

		// then
		List<TeamInviteEntity> invites = teamInviteEntityRepository.findAll();
		assertThat(invites).hasSize(1);
		TeamInviteEntity savedInvite = invites.get(0);
		assertThat(savedInvite.getTeam().getId()).isEqualTo(team.getId());
		assertThat(savedInvite.getInviter().getId()).isEqualTo(user.getId());
		assertThat(savedInvite.getTokenDuration()).isEqualTo(InviteTokenDuration.ONE_DAY);
		assertThat(savedInvite.getToken()).isNotBlank();
		assertThat(savedInvite.getExpiredAt()).isNotNull();
	}

	@Test
	@WithMockUser
	@DisplayName("팀 초대 코드 생성 API 통합 테스트 - 성공: 3일 만료")
	void createTeamInviteCode_Success_ThreeDays() throws Exception {
		// given
		UserEntity user = createTestUser("test@example.com", "테스트 사용자");
		TeamEntity team = createTeamWithOwner("테스트 팀", user);
		CreateTeamInviteApiRequest request = new CreateTeamInviteApiRequest(InviteTokenDuration.THREE_DAYS);

		// when & then
		mockMvc.perform(post("/api/v1/teams/{teamId}/invite", team.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.inviteCode").exists());

		// then
		List<TeamInviteEntity> invites = teamInviteEntityRepository.findAll();
		assertThat(invites).hasSize(1);
		assertThat(invites.get(0).getTokenDuration()).isEqualTo(InviteTokenDuration.THREE_DAYS);
	}

	@Test
	@WithMockUser
	@DisplayName("팀 초대 코드 생성 API 통합 테스트 - 성공: 7일 만료")
	void createTeamInviteCode_Success_SevenDays() throws Exception {
		// given
		UserEntity user = createTestUser("test@example.com", "테스트 사용자");
		TeamEntity team = createTeamWithOwner("테스트 팀", user);
		CreateTeamInviteApiRequest request = new CreateTeamInviteApiRequest(InviteTokenDuration.SEVEN_DAYS);

		// when & then
		mockMvc.perform(post("/api/v1/teams/{teamId}/invite", team.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.inviteCode").exists());

		// then
		List<TeamInviteEntity> invites = teamInviteEntityRepository.findAll();
		assertThat(invites).hasSize(1);
		assertThat(invites.get(0).getTokenDuration()).isEqualTo(InviteTokenDuration.SEVEN_DAYS);
	}

	@Test
	@WithMockUser
	@DisplayName("팀 초대 코드 생성 API 통합 테스트 - 성공: 만료 없음")
	void createTeamInviteCode_Success_Never() throws Exception {
		// given
		UserEntity user = createTestUser("test@example.com", "테스트 사용자");
		TeamEntity team = createTeamWithOwner("테스트 팀", user);
		CreateTeamInviteApiRequest request = new CreateTeamInviteApiRequest(InviteTokenDuration.NO_EXPIRATION);

		// when & then
		mockMvc.perform(post("/api/v1/teams/{teamId}/invite", team.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.inviteCode").exists());

		// then
		List<TeamInviteEntity> invites = teamInviteEntityRepository.findAll();
		assertThat(invites).hasSize(1);
		TeamInviteEntity savedInvite = invites.get(0);
		assertThat(savedInvite.getTokenDuration()).isEqualTo(InviteTokenDuration.NO_EXPIRATION);
		assertThat(savedInvite.getExpiredAt()).isNull(); // 만료 없음이므로 null
	}

	@Test
	@WithMockUser
	@DisplayName("팀 초대 코드 생성 API 통합 테스트 - 실패: 존재하지 않는 팀")
	void createTeamInviteCode_Failure_TeamNotFound() throws Exception {
		// given
		UserEntity user = createTestUser("test@example.com", "테스트 사용자");
		Long nonExistentTeamId = 999L;
		CreateTeamInviteApiRequest request = new CreateTeamInviteApiRequest(InviteTokenDuration.ONE_DAY);

		// when & then
		mockMvc.perform(post("/api/v1/teams/{teamId}/invite", nonExistentTeamId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNotFound());

		// then
		assertThat(teamInviteEntityRepository.findAll()).isEmpty();
	}

	@Test
	@WithMockUser
	@DisplayName("팀 초대 코드 생성 API 통합 테스트 - 실패: 팀 소유자가 아님")
	void createTeamInviteCode_Failure_NotOwner() throws Exception {
		// given
		UserEntity requestUser = createTestUser("requester@example.com", "요청자");
		UserEntity teamOwner = createTestUser("owner@example.com", "팀 소유자");
		TeamEntity team = createTeamWithOwner("테스트 팀", teamOwner);
		CreateTeamInviteApiRequest request = new CreateTeamInviteApiRequest(InviteTokenDuration.ONE_DAY);

		// when & then
		mockMvc.perform(post("/api/v1/teams/{teamId}/invite", team.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNotFound()); // 팀 멤버가 아니므로 EntityNotFoundException

		// then
		assertThat(teamInviteEntityRepository.findAll()).isEmpty();
	}

	@Test
	@WithMockUser
	@DisplayName("팀 초대 코드 생성 API 통합 테스트 - 실패: 유효하지 않은 duration")
	void createTeamInviteCode_Failure_InvalidDuration() throws Exception {
		// given
		UserEntity user = createTestUser("test@example.com", "테스트 사용자");
		TeamEntity team = createTeamWithOwner("테스트 팀", user);
		String invalidRequest = "{\"duration\": \"INVALID_DURATION\"}";

		// when & then
		mockMvc.perform(post("/api/v1/teams/{teamId}/invite", team.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidRequest))
			.andExpect(status().isBadRequest());

		// then
		assertThat(teamInviteEntityRepository.findAll()).isEmpty();
	}

	@Test
	@WithMockUser
	@DisplayName("팀 초대 코드 생성 API 통합 테스트 - 실패: duration 누락")
	void createTeamInviteCode_Failure_MissingDuration() throws Exception {
		// given
		UserEntity user = createTestUser("test@example.com", "테스트 사용자");
		TeamEntity team = createTeamWithOwner("테스트 팀", user);
		String requestWithoutDuration = "{}";

		// when & then
		mockMvc.perform(post("/api/v1/teams/{teamId}/invite", team.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestWithoutDuration))
			.andExpect(status().isBadRequest());

		// then
		assertThat(teamInviteEntityRepository.findAll()).isEmpty();
	}

	// Helper methods
	private UserEntity createTestUser(String email, String name) {
		UserEntity user = UserEntity.socialLoginUser(email, name, "providerId", GOOGLE);
		return userEntityRepository.save(user);
	}

	private TeamEntity createTeamWithOwner(String teamName, UserEntity owner) {
		TeamEntity team = TeamEntity.of(teamName, owner.getId());
		team = teamEntityRepository.save(team);

		TeamUserEntity teamUser = TeamUserEntity.createOwnerUser(owner, team);
		teamUserEntityRepository.save(teamUser);

		return team;
	}
}
