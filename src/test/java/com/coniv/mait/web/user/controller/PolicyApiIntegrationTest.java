package com.coniv.mait.web.user.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

import com.coniv.mait.domain.user.entity.PolicyEntity;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.entity.UserPolicyCheckHistory;
import com.coniv.mait.domain.user.enums.LoginProvider;
import com.coniv.mait.domain.user.enums.PolicyCategory;
import com.coniv.mait.domain.user.enums.PolicyTiming;
import com.coniv.mait.domain.user.enums.PolicyType;
import com.coniv.mait.domain.user.repository.PolicyEntityRepository;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.domain.user.repository.UserPolicyCheckHistoryRepository;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

public class PolicyApiIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	JwtAuthorizationFilter jwtAuthenticationFilter;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PolicyEntityRepository policyEntityRepository;

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private UserPolicyCheckHistoryRepository userPolicyCheckHistoryRepository;

	@AfterEach
	void clear() {
		userPolicyCheckHistoryRepository.deleteAll();
		policyEntityRepository.deleteAll();
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
	@DisplayName("타이밍별 최신 정책 목록 조회 API 통합 테스트 - 성공")
	void findLatestPolicies_Success() throws Exception {
		// given - 정책 생성 (같은 코드의 여러 버전)
		PolicyEntity policyV1 = PolicyEntity.builder()
			.title("서비스 이용약관")
			.policyType(PolicyType.ESSENTIAL)
			.category(PolicyCategory.TERMS_OF_SERVICE)
			.timing(PolicyTiming.SIGN_UP)
			.code("TERMS_SERVICE")
			.version(1)
			.content("서비스 이용약관 내용...")
			.build();
		policyEntityRepository.save(policyV1);

		PolicyEntity policyV2 = PolicyEntity.builder()
			.title("서비스 이용약관")
			.policyType(PolicyType.ESSENTIAL)
			.category(PolicyCategory.TERMS_OF_SERVICE)
			.timing(PolicyTiming.SIGN_UP)
			.code("TERMS_SERVICE")
			.version(2)
			.content("서비스 이용약관 내용 v2...")
			.build();
		policyEntityRepository.save(policyV2);

		PolicyEntity policy2 = PolicyEntity.builder()
			.title("개인정보 처리방침")
			.policyType(PolicyType.ESSENTIAL)
			.category(PolicyCategory.PERSONAL_INFORMATION)
			.timing(PolicyTiming.SIGN_UP)
			.code("PRIVACY_POLICY")
			.version(1)
			.content("개인정보 처리방침 내용...")
			.build();
		policyEntityRepository.save(policy2);

		// when & then
		mockMvc.perform(get("/api/v1/policies")
				.param("timing", PolicyTiming.SIGN_UP.name())
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(2)) // 최신 버전만 2개
			.andExpect(jsonPath("$.data[0].title").exists())
			.andExpect(jsonPath("$.data[0].content").exists())
			.andExpect(jsonPath("$.data[0].policyType").exists());
	}

	@Test
	@Transactional
	@WithCustomUser(email = "test@example.com", name = "테스트유저")
	@DisplayName("타이밍별 최신 정책 목록 조회 API 통합 테스트 - 정책이 없는 경우")
	void findLatestPolicies_EmptyList() throws Exception {
		// given - 정책이 없는 상태

		// when & then
		mockMvc.perform(get("/api/v1/policies")
				.param("timing", PolicyTiming.SIGN_UP.name())
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(0));
	}

	@Test
	@Transactional
	@WithCustomUser(email = "test@example.com", name = "테스트유저")
	@DisplayName("미확인 정책 목록 조회 API 통합 테스트 - 일부 정책만 확인한 경우")
	void findUnConfirmedPolicies_Success() throws Exception {
		// given - 사용자 생성
		UserEntity user = UserEntity.socialLoginUser(
			"user@example.com",
			"사용자",
			"socialId",
			LoginProvider.GOOGLE
		);
		userEntityRepository.save(user);

		// 정책 생성
		PolicyEntity policy1 = PolicyEntity.builder()
			.title("서비스 이용약관")
			.policyType(PolicyType.ESSENTIAL)
			.category(PolicyCategory.TERMS_OF_SERVICE)
			.timing(PolicyTiming.SIGN_UP)
			.code("TERMS_SERVICE")
			.version(1)
			.content("서비스 이용약관 내용...")
			.build();
		policyEntityRepository.save(policy1);

		PolicyEntity policy2 = PolicyEntity.builder()
			.title("개인정보 처리방침")
			.policyType(PolicyType.ESSENTIAL)
			.category(PolicyCategory.PERSONAL_INFORMATION)
			.timing(PolicyTiming.SIGN_UP)
			.code("PRIVACY_POLICY")
			.version(1)
			.content("개인정보 처리방침 내용...")
			.build();
		policyEntityRepository.save(policy2);

		PolicyEntity policy3 = PolicyEntity.builder()
			.title("마케팅 수신 동의")
			.policyType(PolicyType.OPTIONAL)
			.category(PolicyCategory.MARKETING)
			.timing(PolicyTiming.SIGN_UP)
			.code("MARKETING_AGREE")
			.version(1)
			.content("마케팅 수신 동의 내용...")
			.build();
		policyEntityRepository.save(policy3);

		// 사용자가 policy1만 확인
		UserPolicyCheckHistory history = UserPolicyCheckHistory.of(
			true,
			user,
			policy1,
			java.time.LocalDateTime.now()
		);
		userPolicyCheckHistoryRepository.save(history);

		// when & then
		mockMvc.perform(get("/api/v1/policies/unchecked/" + user.getId())
				.param("timing", PolicyTiming.SIGN_UP.name())
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(2)) // policy2, policy3만 반환
			.andExpect(jsonPath("$.data[0].title").exists());
	}

	@Test
	@Transactional
	@WithCustomUser(email = "test@example.com", name = "테스트유저")
	@DisplayName("미확인 정책 목록 조회 API 통합 테스트 - 모든 정책을 확인한 경우")
	void findUnConfirmedPolicies_AllConfirmed() throws Exception {
		// given
		UserEntity user = UserEntity.socialLoginUser(
			"user@example.com",
			"사용자",
			"socialId",
			LoginProvider.GOOGLE
		);
		userEntityRepository.save(user);

		PolicyEntity policy1 = PolicyEntity.builder()
			.title("서비스 이용약관")
			.policyType(PolicyType.ESSENTIAL)
			.category(PolicyCategory.TERMS_OF_SERVICE)
			.timing(PolicyTiming.SIGN_UP)
			.code("TERMS_SERVICE")
			.version(1)
			.content("서비스 이용약관 내용...")
			.build();
		policyEntityRepository.save(policy1);

		UserPolicyCheckHistory history = UserPolicyCheckHistory.of(true, user, policy1, java.time.LocalDateTime.now());
		userPolicyCheckHistoryRepository.save(history);

		// when & then
		mockMvc.perform(get("/api/v1/policies/unchecked/" + user.getId())
				.param("timing", PolicyTiming.SIGN_UP.name())
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(0));
	}
}

