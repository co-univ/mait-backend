package com.coniv.mait.web.team.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.coniv.mait.domain.auth.service.Oauth2UserService;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamInvitationApplicantEntity;
import com.coniv.mait.domain.team.entity.TeamInvitationLinkEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamInvitationApplicationEntityRepository;
import com.coniv.mait.domain.team.repository.TeamInvitationEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.team.service.TeamService;
import com.coniv.mait.domain.team.service.component.InviteTokenGenerator;
import com.coniv.mait.domain.team.service.component.TeamReader;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.domain.user.service.component.UserReader;
import com.coniv.mait.global.auth.jwt.JwtAuthenticationEntryPoint;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.global.config.SecurityConfig;
import com.coniv.mait.global.enums.InviteTokenDuration;
import com.coniv.mait.global.event.MaitEventPublisher;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;
import com.coniv.mait.global.oauth.OAuth2SuccessHandler;

import jakarta.servlet.FilterChain;

@WebMvcTest(controllers = TeamController.class)
@Import({TeamService.class, TeamRoleValidator.class, TeamReader.class, UserReader.class,
	SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class TeamInformationAuthorizationTest {

	private static final Long TEAM_ID = 10L;
	private static final Long REQUESTER_ID = 100L;
	private static final Long INVITATION_ID = 20L;
	private static final String TOKEN = "PRIVATE_INVITATION_TOKEN";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TeamEntityRepository teamEntityRepository;

	@MockitoBean
	private TeamUserEntityRepository teamUserEntityRepository;

	@MockitoBean
	private TeamInvitationEntityRepository teamInvitationEntityRepository;

	@MockitoBean
	private TeamInvitationApplicationEntityRepository teamInvitationApplicationEntityRepository;

	@MockitoBean
	private UserEntityRepository userEntityRepository;

	@MockitoBean
	private QuestionSetEntityRepository questionSetEntityRepository;

	@MockitoBean
	private InviteTokenGenerator inviteTokenGenerator;

	@MockitoBean
	private MaitEventPublisher maitEventPublisher;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@MockitoBean
	private IdempotencyInterceptor idempotencyInterceptor;

	@MockitoBean
	private Oauth2UserService oauth2UserService;

	@MockitoBean
	private OAuth2SuccessHandler oauth2SuccessHandler;

	@MockitoBean
	private ClientRegistrationRepository clientRegistrationRepository;

	private TeamEntity team;
	private UserEntity requester;
	private UserEntity applicant;
	private TeamInvitationLinkEntity invitation;
	private LocalDateTime originalExpiration;

	@BeforeEach
	void setUp() throws Exception {
		team = TeamEntity.ofGroup("대상 팀", 1L);
		ReflectionTestUtils.setField(team, "id", TEAM_ID);
		requester = UserEntity.localLoginUser("requester@example.com", "password", "요청자", "요청자");
		ReflectionTestUtils.setField(requester, "id", REQUESTER_ID);
		applicant = UserEntity.localLoginUser("applicant@example.com", "password", "가입 대기자", "대기자");
		ReflectionTestUtils.setField(applicant, "id", 200L);
		invitation = TeamInvitationLinkEntity.createInvite(requester, team, TOKEN,
			InviteTokenDuration.ONE_DAY, TeamUserRole.PLAYER, true);
		ReflectionTestUtils.setField(invitation, "id", INVITATION_ID);
		originalExpiration = invitation.getExpiredAt();
		when(teamEntityRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
		when(userEntityRepository.findById(REQUESTER_ID)).thenReturn(Optional.of(requester));
		when(teamInvitationEntityRepository.findById(INVITATION_ID)).thenReturn(Optional.of(invitation));
		when(idempotencyInterceptor.preHandle(any(), any(), any())).thenReturn(true);
		doAnswer(invocation -> {
			FilterChain chain = invocation.getArgument(2);
			chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
			return null;
		}).when(jwtAuthorizationFilter).doFilter(any(), any(), any());
	}

	@ParameterizedTest
	@MethodSource("authorizationCases")
	@DisplayName("팀원 목록은 해당 팀 멤버에게, 대기자·초대 링크 관리는 해당 팀 OWNER·MAKER에게만 허용한다")
	void enforceInformationAccess(String operation, String role) throws Exception {
		// given
		boolean member = role.equals("OWNER") || role.equals("MAKER") || role.equals("PLAYER");
		boolean manager = role.equals("OWNER") || role.equals("MAKER");
		boolean allowed = operation.equals("USERS") ? member : manager;
		prepareMembership(role, member);
		prepareResponseData();

		// when & then
		var result = mockMvc.perform(authenticatedRequest(operation))
			.andExpect(status().is(allowed ? 200 : 403))
			.andExpect(jsonPath("$.isSuccess").value(allowed));
		if (allowed) {
			switch (operation) {
				case "USERS" -> result.andExpect(jsonPath("$.data[0].name").value("요청자"));
				case "APPLICANTS" -> result.andExpect(jsonPath("$.data[0].name").value("가입 대기자"));
				case "INVITATIONS" -> result.andExpect(jsonPath("$.data[0].token").value(TOKEN));
				case "REVOKE" -> assertThat(invitation.getExpiredAt()).isBefore(originalExpiration);
				default -> throw new IllegalArgumentException(operation);
			}
		} else {
			result.andExpect(jsonPath("$.data").doesNotExist());
			verifyNoProtectedDataRead();
		}
		if (!allowed || !operation.equals("REVOKE")) {
			assertThat(invitation.getExpiredAt()).isEqualTo(originalExpiration);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"USERS", "APPLICANTS", "INVITATIONS", "REVOKE"})
	@DisplayName("익명 사용자는 팀 내부 정보 조회와 초대 링크 폐기를 할 수 없다")
	void rejectAnonymous(String operation) throws Exception {
		mockMvc.perform(request(operation).with(anonymous()))
			.andExpect(status().isUnauthorized());
		verifyNoInteractions(teamEntityRepository, userEntityRepository, teamUserEntityRepository,
			teamInvitationEntityRepository, teamInvitationApplicationEntityRepository);
		assertThat(invitation.getExpiredAt()).isEqualTo(originalExpiration);
	}

	@ParameterizedTest
	@ValueSource(strings = {"USERS", "APPLICANTS", "INVITATIONS", "REVOKE"})
	@DisplayName("삭제된 팀의 내부 정보와 초대 링크에는 접근할 수 없다")
	void rejectDeletedTeam(String operation) throws Exception {
		team.markDeleted();
		mockMvc.perform(authenticatedRequest(operation))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("T-001"));
		verifyNoProtectedDataRead();
		assertThat(invitation.getExpiredAt()).isEqualTo(originalExpiration);
	}

	@Test
	@DisplayName("초대 링크 폐기 시 요청자가 제시한 다른 팀 ID로 권한 검사를 우회할 수 없다")
	void revokeUsesInvitationTeam() throws Exception {
		prepareMembership("OTHER_OWNER", false);
		mockMvc.perform(authenticatedRequest("REVOKE").param("teamId", "99").param("userId", "1"))
			.andExpect(status().isForbidden());
		assertThat(invitation.getExpiredAt()).isEqualTo(originalExpiration);
	}

	@Test
	@DisplayName("유효한 초대 토큰을 가진 익명 사용자는 가입 전 팀 미리보기를 조회할 수 있다")
	void preserveInvitationPreview() throws Exception {
		when(teamInvitationEntityRepository.findByTokenFetchJoinTeam(TOKEN)).thenReturn(Optional.of(invitation));
		mockMvc.perform(get("/api/v1/teams/invitation/info").param("code", TOKEN).with(anonymous()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.teamId").value(TEAM_ID))
			.andExpect(jsonPath("$.data.teamName").value("대상 팀"))
			.andExpect(jsonPath("$.data.token").doesNotExist())
			.andExpect(jsonPath("$.data.users").doesNotExist());
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	@DisplayName("초대 팀 미리보기는 잘못된 토큰이나 만료된 토큰을 거절한다")
	void rejectInvalidPreviewToken(boolean expired) throws Exception {
		if (expired) {
			ReflectionTestUtils.setField(invitation, "expiredAt", LocalDateTime.now().minusMinutes(1));
			when(teamInvitationEntityRepository.findByTokenFetchJoinTeam(TOKEN)).thenReturn(Optional.of(invitation));
		}
		mockMvc.perform(get("/api/v1/teams/invitation/info").param("code", TOKEN).with(anonymous()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	private void prepareMembership(String role, boolean member) {
		when(teamUserEntityRepository.existsByTeamAndUser(team, requester)).thenReturn(member);
		if (member) {
			when(teamUserEntityRepository.findByTeamAndUser(team, requester))
				.thenReturn(Optional.of(TeamUserEntity.createTeamUser(requester, team, TeamUserRole.valueOf(role))));
		} else if (role.startsWith("OTHER_")) {
			TeamEntity otherTeam = TeamEntity.ofGroup("다른 팀", REQUESTER_ID);
			ReflectionTestUtils.setField(otherTeam, "id", 99L);
			when(teamUserEntityRepository.findByTeamAndUser(otherTeam, requester)).thenReturn(Optional.of(
				TeamUserEntity.createTeamUser(requester, otherTeam, TeamUserRole.valueOf(role.substring(6)))));
		}
	}

	private void prepareResponseData() {
		when(teamUserEntityRepository.findAllByTeamIdFetchJoinUser(TEAM_ID))
			.thenReturn(List.of(TeamUserEntity.createPlayerUser(requester, team)));
		when(teamInvitationEntityRepository.findActiveLinksByTeam(eq(team), any(LocalDateTime.class)))
			.thenReturn(List.of(invitation));
		when(teamInvitationApplicationEntityRepository.findAllByTeamIdAndApplicationStatus(eq(TEAM_ID), any()))
			.thenReturn(List.of(TeamInvitationApplicantEntity.createApplication(TEAM_ID, applicant.getId(),
				INVITATION_ID, TeamUserRole.PLAYER, LocalDateTime.now())));
		when(userEntityRepository.findAllById(List.of(applicant.getId()))).thenReturn(List.of(applicant));
	}

	private void verifyNoProtectedDataRead() {
		verify(teamUserEntityRepository, never()).findAllByTeamIdFetchJoinUser(any());
		verify(teamInvitationEntityRepository, never()).findActiveLinksByTeam(any(), any());
		verifyNoInteractions(teamInvitationApplicationEntityRepository);
		verify(userEntityRepository, never()).findAllById(any());
	}

	private MockHttpServletRequestBuilder authenticatedRequest(String operation) {
		var principal = MaitUser.builder().id(REQUESTER_ID).build();
		return request(operation)
			.with(authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of())));
	}

	private MockHttpServletRequestBuilder request(String operation) {
		return switch (operation) {
			case "USERS" -> get("/api/v1/teams/{teamId}/users", TEAM_ID);
			case "APPLICANTS" -> get("/api/v1/teams/{teamId}/applicants", TEAM_ID);
			case "INVITATIONS" -> get("/api/v1/teams/{teamId}/invitations", TEAM_ID);
			case "REVOKE" -> delete("/api/v1/teams/invitations/{invitationId}", INVITATION_ID);
			default -> throw new IllegalArgumentException(operation);
		};
	}

	private static Stream<Arguments> authorizationCases() {
		return Stream.of("USERS", "APPLICANTS", "INVITATIONS", "REVOKE").flatMap(operation ->
			Stream.of("OWNER", "MAKER", "PLAYER", "OUTSIDER", "OTHER_OWNER", "OTHER_MAKER")
				.map(role -> Arguments.of(operation, role)));
	}
}
