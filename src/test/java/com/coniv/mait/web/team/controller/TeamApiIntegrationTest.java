package com.coniv.mait.web.team.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamInviteApplicantEntity;
import com.coniv.mait.domain.team.entity.TeamInviteEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.InviteApplicationStatus;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamInviteApplicationEntityRepository;
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

	@Autowired
	private TeamInviteApplicationEntityRepository teamInviteApplicationEntityRepository;

	@AfterEach
	void clear() {
		teamInviteApplicationEntityRepository.deleteAll();
		teamInviteEntityRepository.deleteAll();
		teamUserEntityRepository.deleteAll();
		teamEntityRepository.deleteAll();
		userEntityRepository.deleteAll();
	}

	@BeforeEach
	void passThroughJwtFilter() throws Exception {
		Mockito.doAnswer(inv -> {
			ServletRequest request = inv.getArgument(0);
			ServletResponse response = inv.getArgument(1);
			FilterChain chain = inv.getArgument(2);
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
	@Transactional
	@WithCustomUser(email = "test@example.com", name = "사용자1")
	@DisplayName("팀 초대 코드 생성 API 통합 테스트 - 성공")
	void createTeamInviteCode_Success() throws Exception {
		// given
		UserEntity user = userEntityRepository.findByEmail("test@example.com").orElseThrow();
		TeamEntity team = createTeamWithOwner("테스트 팀", user);
		CreateTeamInviteApiRequest request = new CreateTeamInviteApiRequest(InviteTokenDuration.ONE_DAY,
			TeamUserRole.PLAYER);

		// when & then
		mockMvc.perform(post("/api/v1/teams/{teamId}/invite", team.getId())
				.param("requiresApproval", "false")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data.token").exists());

		// then
		List<TeamInviteEntity> invites = teamInviteEntityRepository.findAll();
		assertThat(invites).hasSize(1);
		TeamInviteEntity savedInvite = invites.get(0);
		assertThat(savedInvite.getTeam().getId()).isEqualTo(team.getId());
		assertThat(savedInvite.getInvitor().getId()).isEqualTo(user.getId());
		assertThat(savedInvite.getTokenDuration()).isEqualTo(InviteTokenDuration.ONE_DAY);
		assertThat(savedInvite.getToken()).isNotBlank();
		assertThat(savedInvite.getExpiredAt()).isNotNull();
		// 새로운 필드 검증
		assertThat(savedInvite.getRoleOnJoin()).isEqualTo(TeamUserRole.PLAYER);
		assertThat(savedInvite.isRequiresApproval()).isFalse();
	}

	@Test
	@Transactional
	@DisplayName("팀 초대 정보 조회 API 통합 테스트 - 익명 사용자 성공")
	void getTeamInfo_Anonymous_Success() throws Exception {
		// given: create owner user and team and invite
		UserEntity owner = UserEntity.socialLoginUser("owner@example.com", "오너", "provider", null);
		userEntityRepository.save(owner);
		TeamEntity team = createTeamWithOwner("익명초대팀", owner);

		String token = "ANON" + System.currentTimeMillis();
		TeamInviteEntity invite = TeamInviteEntity.createInvite(owner, team, token, InviteTokenDuration.ONE_DAY,
			TeamUserRole.PLAYER, false);
		teamInviteEntityRepository.save(invite);

		// when & then: call without authentication
		mockMvc.perform(get("/api/v1/teams/invite/info").param("code", token)
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data.teamId").value(team.getId()))
			.andExpect(jsonPath("$.data.teamName").value(team.getName()))
			.andExpect(jsonPath("$.data.requiresApproval").value(false))
			.andExpect(jsonPath("$.data.applicationStatus").doesNotExist());
	}

	@Test
	@Transactional
	@WithCustomUser(email = "applicant@example.com", name = "신청자")
	@DisplayName("팀 초대 정보 조회 API 통합 테스트 - 인증된 사용자, 신청 기록 존재")
	void getTeamInfo_Authenticated_WithApplication() throws Exception {
		// given: owner, team, invite and applicant user
		UserEntity owner = UserEntity.socialLoginUser("owner2@example.com", "오너2", "provider", null);
		userEntityRepository.save(owner);
		UserEntity applicant = userEntityRepository.findByEmail("applicant@example.com").orElseThrow();

		TeamEntity team = createTeamWithOwner("신청팀", owner);
		String token = "APP" + System.currentTimeMillis();
		TeamInviteEntity invite = TeamInviteEntity.createInvite(owner, team, token, InviteTokenDuration.ONE_DAY,
			TeamUserRole.PLAYER, true);
		teamInviteEntityRepository.save(invite);

		// create application record
		TeamInviteApplicantEntity application = TeamInviteApplicantEntity.builder()
			.teamId(team.getId())
			.userId(applicant.getId())
			.inviteId(invite.getId())
			.role(TeamUserRole.PLAYER)
			.appliedAt(LocalDateTime.now())
			.applicationStatus(InviteApplicationStatus.PENDING)
			.build();
		teamInviteApplicationEntityRepository.save(application);

		// when & then: authenticated request (WithCustomUser will set the principal)
		mockMvc.perform(get("/api/v1/teams/invite/info").param("code", token)
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data.teamId").value(team.getId()))
			.andExpect(jsonPath("$.data.teamName").value(team.getName()))
			.andExpect(jsonPath("$.data.requiresApproval").value(true))
			.andExpect(jsonPath("$.data.applicationStatus").value("PENDING"));
	}

	private TeamEntity createTeamWithOwner(String teamName, UserEntity owner) {
		TeamEntity team = TeamEntity.of(teamName, owner.getId());
		team = teamEntityRepository.save(team);

		TeamUserEntity teamUser = TeamUserEntity.createOwnerUser(owner, team);
		teamUserEntityRepository.save(teamUser);

		return team;
	}
}
