package com.coniv.mait.global.auth;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import com.coniv.mait.domain.onboarding.repository.OnboardingScreenRepository;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.enums.UserRole;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.auth.jwt.JwtTokenProvider;
import com.coniv.mait.global.auth.jwt.repository.BlackListRepository;
import com.coniv.mait.web.integration.BaseIntegrationTest;

import jakarta.persistence.EntityManager;

class AdminApiAuthorizationIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private TeamEntityRepository teamEntityRepository;

	@Autowired
	private TeamUserEntityRepository teamUserEntityRepository;

	@Autowired
	private OnboardingScreenRepository onboardingScreenRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@MockitoBean
	private BlackListRepository blackListRepository;

	@Test
	@DisplayName("DB에 저장된 관리자 역할로 인증해 온보딩 카탈로그를 실제 저장한다")
	void adminCreatesScreenWithPersistedRole() throws Exception {
		// given
		UserEntity admin = saveUser(UserRole.ADMIN);
		String token = jwtTokenProvider.createToken(admin.getId()).accessToken();
		entityManager.clear();
		String body = """
			{"code":"ADMIN_AUTH_TEST","title":"관리자 등록 화면"}
			""";

		// when & then
		mockMvc.perform(post("/api/v1/admin/onboarding/screens")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.code").value("ADMIN_AUTH_TEST"));
		assertThat(onboardingScreenRepository.findByCode("ADMIN_AUTH_TEST")).hasValueSatisfying(screen -> {
			assertThat(screen.getTitle()).isEqualTo("관리자 등록 화면");
			assertThat(screen.isExposed()).isTrue();
		});
	}

	@ParameterizedTest
	@EnumSource(TeamUserRole.class)
	@DisplayName("팀 OWNER·MAKER·PLAYER 모두 다른 팀의 관리자 API 접근 권한은 없다")
	void teamRoleCannotGrantAdminAccess(TeamUserRole teamRole) throws Exception {
		// given
		UserEntity user = saveUser(UserRole.USER);
		TeamEntity ownTeam = teamEntityRepository.save(TeamEntity.ofGroup("내 팀", user.getId()));
		TeamEntity otherTeam = teamEntityRepository.save(TeamEntity.ofGroup("다른 팀", user.getId() + 1));
		teamUserEntityRepository.saveAndFlush(TeamUserEntity.createTeamUser(user, ownTeam, teamRole));
		String token = jwtTokenProvider.createToken(user.getId()).accessToken();
		entityManager.clear();

		// when & then
		mockMvc.perform(get("/api/v1/admin/teams/{teamId}/question-sets", otherTeam.getId())
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("C-008"));
	}

	@Test
	@DisplayName("DB에서 관리자 권한을 회수하면 토큰 재발급 없이 다음 요청이 거절된다")
	void databaseRoleRevocationAppliesToExistingToken() throws Exception {
		// given
		UserEntity admin = saveUser(UserRole.ADMIN);
		String token = jwtTokenProvider.createToken(admin.getId()).accessToken();
		entityManager.clear();
		mockMvc.perform(get("/api/v1/admin/teams").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk());

		// when
		jdbcTemplate.update("UPDATE users SET role = 'USER' WHERE id = ?", admin.getId());
		entityManager.clear();

		// then
		mockMvc.perform(get("/api/v1/admin/teams").header("Authorization", "Bearer " + token))
			.andExpect(status().isForbidden());
	}

	private UserEntity saveUser(UserRole role) {
		UserEntity user = UserEntity.localLoginUser("admin-auth@example.com", "password", "사용자", "닉네임");
		ReflectionTestUtils.setField(user, "role", role);
		return userEntityRepository.saveAndFlush(user);
	}
}
