package com.coniv.mait.web.user.controller;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.auth.dto.OauthPendingPayload;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.TeamType;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.PolicyEntity;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.enums.PolicyCategory;
import com.coniv.mait.domain.user.enums.PolicyTiming;
import com.coniv.mait.domain.user.enums.PolicyType;
import com.coniv.mait.domain.user.repository.PolicyEntityRepository;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.auth.jwt.cache.OauthPendingRedisRepository;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;
import com.coniv.mait.web.user.dto.PolicyCheckRequest;
import com.coniv.mait.web.user.dto.SignUpApiRequest;
import com.coniv.mait.web.user.dto.UpdateNicknameRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;

public class UserApiIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	JwtAuthorizationFilter jwtAuthenticationFilter;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private PolicyEntityRepository policyEntityRepository;

	@Autowired
	private TeamEntityRepository teamEntityRepository;

	@Autowired
	private TeamUserEntityRepository teamUserEntityRepository;

	@Autowired
	private OauthPendingRedisRepository oauthPendingRedisRepository;

	@AfterEach
	void clear() {
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
	@WithCustomUser(email = "test@example.com", name = "테스트유저")
	@DisplayName("사용자 정보 반환 API 통합 테스트")
	void getUserInfo_Success() throws Exception {
		// when & then
		mockMvc.perform(get("/api/v1/users/me")
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data.email").value("test@example.com"))
			.andExpect(jsonPath("$.data.name").value("테스트유저"));
	}

	@Test
	@Transactional
	@WithCustomUser(email = "test@example.com", name = "테스트유저")
	@DisplayName("닉네임 변경 API 통합 테스트")
	void updateUserNickname_Success() throws Exception {
		// given
		UpdateNicknameRequest request = new UpdateNicknameRequest("빠른고양이");

		// when & then
		mockMvc.perform(patch("/api/v1/users/nickname")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data.nickname").value("빠른고양이"))
			.andExpect(jsonPath("$.data.fullNickname").exists());

		// then
		UserEntity user = userEntityRepository.findByEmail("test@example.com").orElseThrow();
		assertThat(user.getNickname()).isEqualTo("빠른고양이");
		assertThat(user.getNicknameCode()).isNotNull();
		assertThat(user.getNicknameCode()).hasSize(4);
		assertThat(user.getFullNickname()).startsWith("빠른고양이#");
	}

	@Test
	@Transactional
	@WithCustomUser(email = "test@example.com", name = "테스트유저")
	@DisplayName("랜덤 닉네임 반환 API 통합 테스트 - 성공")
	void getRandomNickname_Success() throws Exception {
		// when & then
		mockMvc.perform(get("/api/v1/users/nickname/random")
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data.nickname").exists())
			.andExpect(jsonPath("$.data.nickname").isNotEmpty());
	}

	@Test
	@Transactional
	@DisplayName("회원가입 API 통합 테스트 - 가입 완료 시 개인 워크스페이스가 함께 생성된다")
	void signup_Success_CreatesPersonalWorkspace() throws Exception {
		// given
		PolicyEntity policy = PolicyEntity.builder()
			.title("서비스 이용약관")
			.policyType(PolicyType.ESSENTIAL)
			.category(PolicyCategory.TERMS_OF_SERVICE)
			.timing(PolicyTiming.SIGN_UP)
			.code("TERMS_SERVICE")
			.version(1)
			.content("내용")
			.build();
		policyEntityRepository.save(policy);

		String signupToken = "test-signup-token";
		OauthPendingPayload payload = OauthPendingPayload.builder()
			.provider("google")
			.providerId("provider-id-123")
			.email("newuser@example.com")
			.name("신규유저")
			.build();
		oauthPendingRedisRepository.save(signupToken, objectMapper.writeValueAsString(payload));

		SignUpApiRequest request = new SignUpApiRequest(
			"신규닉네임",
			List.of(new PolicyCheckRequest(policy.getId(), true))
		);

		// when
		mockMvc.perform(post("/api/v1/users/sign-up")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.cookie(new Cookie("OAUTH_SIGNUP_KEY", signupToken))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true));

		// then
		UserEntity savedUser = userEntityRepository.findByEmail("newuser@example.com").orElseThrow();
		assertThat(savedUser.getNickname()).isEqualTo("신규닉네임");

		List<TeamUserEntity> joinedTeams = teamUserEntityRepository.findAllByUserFetchJoinActiveTeam(savedUser);
		assertThat(joinedTeams).hasSize(1);

		TeamUserEntity personalMembership = joinedTeams.get(0);
		assertThat(personalMembership.getUserRole()).isEqualTo(TeamUserRole.OWNER);

		TeamEntity personalTeam = personalMembership.getTeam();
		assertThat(personalTeam.getType()).isEqualTo(TeamType.PERSONAL);
		assertThat(personalTeam.getCreatorId()).isEqualTo(savedUser.getId());
		assertThat(personalTeam.getName()).isNotBlank();

		assertThat(teamEntityRepository.findById(personalTeam.getId())).isPresent();
	}
}
