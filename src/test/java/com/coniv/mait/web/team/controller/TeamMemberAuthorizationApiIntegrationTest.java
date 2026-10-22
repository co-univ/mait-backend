package com.coniv.mait.web.team.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;
import com.coniv.mait.web.team.dto.AddTeamUserApiRequest;
import com.coniv.mait.web.team.dto.UpdateTeamUserRoleApiRequest;

import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@WithCustomUser
class TeamMemberAuthorizationApiIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private TeamEntityRepository teamEntityRepository;

	@Autowired
	private TeamUserEntityRepository teamUserEntityRepository;

	@Autowired
	private EntityManager entityManager;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	private UserEntity requester;
	private TeamEntity team;
	private TeamUserEntity ownerMember;
	private TeamUserEntity targetMember;
	private UserEntity candidate;

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
	@DisplayName("팀원 관리는 해당 팀의 OWNER·MAKER만 허용하고 PLAYER·다른 팀 관리자는 차단한다")
	void enforceTeamMemberManagementAuthority(String operation, String requesterRole) throws Exception {
		// given
		prepareTeam(requesterRole);
		boolean allowed = requesterRole.equals("OWNER") || requesterRole.equals("MAKER");
		long memberCount = teamUserEntityRepository.countByTeamId(team.getId());

		// when & then
		mockMvc.perform(mutationRequest(operation, TeamUserRole.MAKER))
			.andExpect(status().is(allowed ? 200 : 403))
			.andExpect(jsonPath("$.isSuccess").value(allowed));
		entityManager.flush();
		entityManager.clear();
		assertThat(teamUserEntityRepository.existsByTeamAndUser(team, candidate))
			.isEqualTo(allowed && operation.equals("ADD"));
		if (allowed && operation.equals("DELETE")) {
			assertThat(teamUserEntityRepository.findById(targetMember.getId())).isEmpty();
		} else {
			TeamUserRole expectedRole = allowed && operation.equals("UPDATE")
				? TeamUserRole.MAKER : TeamUserRole.PLAYER;
			assertThat(teamUserEntityRepository.findById(targetMember.getId()))
				.hasValueSatisfying(member -> assertThat(member.getUserRole()).isEqualTo(expectedRole));
		}
		long delta = allowed ? switch (operation) {
			case "ADD" -> 1;
			case "DELETE" -> -1;
			default -> 0;
		} : 0;
		assertThat(teamUserEntityRepository.countByTeamId(team.getId())).isEqualTo(memberCount + delta);
		assertOwnerPreserved();
	}

	@ParameterizedTest
	@MethodSource("ownerProtectionCases")
	@DisplayName("팀 관리자도 기존 OWNER를 삭제하거나 강등할 수 없다")
	void preserveOwner(String operation, String requesterRole) throws Exception {
		// given
		prepareTeam(requesterRole);
		targetMember = ownerMember;
		long memberCount = teamUserEntityRepository.countByTeamId(team.getId());

		// when & then
		mockMvc.perform(mutationRequest(operation, TeamUserRole.PLAYER))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("T-001"));
		entityManager.flush();
		entityManager.clear();
		assertOwnerPreserved();
		assertThat(teamUserEntityRepository.countByTeamId(team.getId())).isEqualTo(memberCount);
	}

	@ParameterizedTest
	@ValueSource(strings = {"ADD", "UPDATE"})
	@DisplayName("OWNER를 추가로 지정하는 요청은 거절하고 기존 역할을 보존한다")
	void cannotAssignNewOwner(String operation) throws Exception {
		// given
		prepareTeam("OWNER");

		// when & then
		mockMvc.perform(mutationRequest(operation, TeamUserRole.OWNER))
			.andExpect(status().isBadRequest());
		entityManager.flush();
		entityManager.clear();
		assertThat(teamUserEntityRepository.existsByTeamAndUser(team, candidate)).isFalse();
		assertThat(teamUserEntityRepository.findById(targetMember.getId()))
			.hasValueSatisfying(member -> assertThat(member.getUserRole()).isEqualTo(TeamUserRole.PLAYER));
		assertOwnerPreserved();
	}

	@ParameterizedTest
	@ValueSource(strings = {"ADD", "DELETE", "UPDATE"})
	@DisplayName("익명 사용자의 팀원 변경 요청은 인증 단계에서 거절한다")
	void anonymousCannotMutateTeamMembers(String operation) throws Exception {
		// given
		prepareTeam("OWNER");
		long memberCount = teamUserEntityRepository.countByTeamId(team.getId());

		// when & then
		mockMvc.perform(mutationRequest(operation, TeamUserRole.MAKER).with(anonymous()))
			.andExpect(status().isUnauthorized());
		entityManager.flush();
		entityManager.clear();
		assertThat(teamUserEntityRepository.countByTeamId(team.getId())).isEqualTo(memberCount);
		assertThat(teamUserEntityRepository.findById(targetMember.getId()))
			.hasValueSatisfying(member -> assertThat(member.getUserRole()).isEqualTo(TeamUserRole.PLAYER));
		assertOwnerPreserved();
	}

	private void prepareTeam(String requesterRole) {
		UserEntity owner = requesterRole.equals("OWNER") ? requester : saveUser("owner");
		team = teamEntityRepository.save(TeamEntity.ofGroup("대상 팀", owner.getId()));
		ownerMember = teamUserEntityRepository.save(TeamUserEntity.createOwnerUser(owner, team));
		if (requesterRole.equals("MAKER") || requesterRole.equals("PLAYER")) {
			teamUserEntityRepository.save(TeamUserEntity.createTeamUser(requester, team,
				TeamUserRole.valueOf(requesterRole)));
		} else if (requesterRole.startsWith("OTHER_")) {
			TeamUserRole otherRole = TeamUserRole.valueOf(requesterRole.substring("OTHER_".length()));
			UserEntity otherOwner = otherRole == TeamUserRole.OWNER ? requester : owner;
			TeamEntity otherTeam = teamEntityRepository.save(TeamEntity.ofGroup("요청자의 다른 팀", otherOwner.getId()));
			if (otherRole != TeamUserRole.OWNER) {
				teamUserEntityRepository.save(TeamUserEntity.createOwnerUser(otherOwner, otherTeam));
			}
			teamUserEntityRepository.save(TeamUserEntity.createTeamUser(requester, otherTeam, otherRole));
		}
		targetMember = teamUserEntityRepository.save(TeamUserEntity.createPlayerUser(saveUser("target"), team));
		candidate = requesterRole.startsWith("OTHER_") ? requester : saveUser("candidate");
	}

	private UserEntity saveUser(String name) {
		return userEntityRepository.save(UserEntity.localLoginUser(name + "@example.com", "password", name, name));
	}

	private MockHttpServletRequestBuilder mutationRequest(String operation, TeamUserRole role) throws Exception {
		return switch (operation) {
			case "ADD" -> post("/api/v1/teams/{teamId}/team-users", team.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new AddTeamUserApiRequest(candidate.getId(), role)));
			case "DELETE" -> delete("/api/v1/teams/team-users/{teamUserId}", targetMember.getId());
			case "UPDATE" -> patch("/api/v1/teams/team-users/{teamUserId}/role", targetMember.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new UpdateTeamUserRoleApiRequest(role)));
			default -> throw new IllegalArgumentException(operation);
		};
	}

	private void assertOwnerPreserved() {
		assertThat(teamUserEntityRepository.findById(ownerMember.getId()))
			.hasValueSatisfying(member -> assertThat(member.getUserRole()).isEqualTo(TeamUserRole.OWNER));
	}

	private static Stream<Arguments> authorizationCases() {
		return Stream.of("ADD", "DELETE", "UPDATE").flatMap(operation ->
			Stream.of("OWNER", "MAKER", "PLAYER", "OTHER_OWNER", "OTHER_MAKER")
				.map(role -> Arguments.of(operation, role)));
	}

	private static Stream<Arguments> ownerProtectionCases() {
		return Stream.of("DELETE", "UPDATE").flatMap(operation ->
			Stream.of("OWNER", "MAKER").map(role -> Arguments.of(operation, role)));
	}
}
