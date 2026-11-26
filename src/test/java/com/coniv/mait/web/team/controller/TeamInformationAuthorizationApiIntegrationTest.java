package com.coniv.mait.web.team.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamInvitationApplicantEntity;
import com.coniv.mait.domain.team.entity.TeamInvitationLinkEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamInvitationApplicationEntityRepository;
import com.coniv.mait.domain.team.repository.TeamInvitationEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.enums.InviteTokenDuration;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;

import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@WithCustomUser
class TeamInformationAuthorizationApiIntegrationTest extends BaseIntegrationTest {

	private static final String INVITATION_TOKEN = "EDU663TOKEN";

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private TeamEntityRepository teamEntityRepository;

	@Autowired
	private TeamUserEntityRepository teamUserEntityRepository;

	@Autowired
	private TeamInvitationEntityRepository teamInvitationEntityRepository;

	@Autowired
	private TeamInvitationApplicationEntityRepository teamInvitationApplicationEntityRepository;

	@Autowired
	private EntityManager entityManager;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	private UserEntity requester;
	private TeamEntity team;
	private TeamInvitationLinkEntity invitation;

	@BeforeEach
	void setUp() throws Exception {
		requester = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		doAnswer(invocation -> {
			ServletRequest request = invocation.getArgument(0);
			ServletResponse response = invocation.getArgument(1);
			FilterChain chain = invocation.getArgument(2);
			chain.doFilter(request, response);
			return null;
		}).when(jwtAuthorizationFilter).doFilter(any(), any(), any());
	}

	@ParameterizedTest
	@MethodSource("authorizationCases")
	@DisplayName("팀원 목록은 팀 멤버만, 가입 대기자·초대 링크 조회·폐기는 해당 팀 OWNER·MAKER만 허용한다")
	void enforceTeamInformationAuthority(String operation, String requesterRole) throws Exception {
		// given
		prepareTeam(requesterRole);
		boolean allowed = allowedRoles(operation).contains(requesterRole);

		// when
		ResultActions result = mockMvc.perform(request(operation));

		// then
		result.andExpect(status().is(allowed ? 200 : 403))
			.andExpect(jsonPath("$.isSuccess").value(allowed));
		if (!allowed) {
			result.andExpect(jsonPath("$.data").doesNotExist());
		} else {
			switch (operation) {
				case "USERS" -> result.andExpect(jsonPath("$.data[?(@.name == 'target')]").exists());
				case "APPLICANTS" -> result.andExpect(jsonPath("$.data[0].name").value("applicant"));
				case "INVITATIONS" -> result.andExpect(jsonPath("$.data[0].token").value(INVITATION_TOKEN));
				default -> {
				}
			}
		}
		entityManager.flush();
		entityManager.clear();
		assertThat(teamInvitationEntityRepository.findById(invitation.getId()))
			.hasValueSatisfying(link -> assertThat(link.getExpiredAt() != null)
				.isEqualTo(allowed && operation.equals("DELETE_INVITATION")));
	}

	@ParameterizedTest
	@ValueSource(strings = {"USERS", "APPLICANTS", "INVITATIONS", "DELETE_INVITATION"})
	@DisplayName("익명 사용자의 팀 정보·초대 링크 요청은 인증 단계에서 거절한다")
	void anonymousCannotAccessTeamInformation(String operation) throws Exception {
		// given
		prepareTeam("OWNER");

		// when & then
		mockMvc.perform(request(operation).with(anonymous()))
			.andExpect(status().isUnauthorized());
		entityManager.flush();
		entityManager.clear();
		assertThat(teamInvitationEntityRepository.findById(invitation.getId()))
			.hasValueSatisfying(link -> assertThat(link.getExpiredAt()).isNull());
	}

	private void prepareTeam(String requesterRole) {
		UserEntity owner = requesterRole.equals("OWNER") ? requester : saveUser("owner");
		team = teamEntityRepository.save(TeamEntity.ofGroup("대상 팀", owner.getId()));
		teamUserEntityRepository.save(TeamUserEntity.createOwnerUser(owner, team));
		if (requesterRole.equals("MAKER") || requesterRole.equals("PLAYER")) {
			teamUserEntityRepository.save(TeamUserEntity.createTeamUser(requester, team,
				TeamUserRole.valueOf(requesterRole)));
		} else if (requesterRole.startsWith("OTHER_")) {
			TeamUserRole otherRole = TeamUserRole.valueOf(requesterRole.substring("OTHER_".length()));
			TeamEntity otherTeam = teamEntityRepository.save(TeamEntity.ofGroup("요청자의 다른 팀", requester.getId()));
			teamUserEntityRepository.save(TeamUserEntity.createTeamUser(requester, otherTeam, otherRole));
		}
		teamUserEntityRepository.save(TeamUserEntity.createPlayerUser(saveUser("target"), team));
		invitation = teamInvitationEntityRepository.save(TeamInvitationLinkEntity.createInvite(owner, team,
			INVITATION_TOKEN, InviteTokenDuration.NO_EXPIRATION, TeamUserRole.PLAYER, true));
		teamInvitationApplicationEntityRepository.save(TeamInvitationApplicantEntity.createApplication(team.getId(),
			saveUser("applicant").getId(), invitation.getId(), TeamUserRole.PLAYER, LocalDateTime.now()));
	}

	private UserEntity saveUser(String name) {
		return userEntityRepository.save(UserEntity.localLoginUser(name + "@example.com", "password", name, name));
	}

	private MockHttpServletRequestBuilder request(String operation) {
		return switch (operation) {
			case "USERS" -> get("/api/v1/teams/{teamId}/users", team.getId());
			case "APPLICANTS" -> get("/api/v1/teams/{teamId}/applicants", team.getId());
			case "INVITATIONS" -> get("/api/v1/teams/{teamId}/invitations", team.getId());
			case "DELETE_INVITATION" -> delete("/api/v1/teams/invitations/{invitationId}", invitation.getId());
			default -> throw new IllegalArgumentException(operation);
		};
	}

	private static Set<String> allowedRoles(String operation) {
		if (operation.equals("USERS")) {
			return Set.of("OWNER", "MAKER", "PLAYER");
		}
		return Set.of("OWNER", "MAKER");
	}

	private static Stream<Arguments> authorizationCases() {
		return Stream.of("USERS", "APPLICANTS", "INVITATIONS", "DELETE_INVITATION").flatMap(operation ->
			Stream.of("OWNER", "MAKER", "PLAYER", "OTHER_OWNER", "OTHER_MAKER", "NON_MEMBER")
				.map(role -> Arguments.of(operation, role)));
	}
}
