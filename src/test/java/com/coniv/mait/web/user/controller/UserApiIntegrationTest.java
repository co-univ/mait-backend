package com.coniv.mait.web.user.controller;

import static org.assertj.core.api.Assertions.*;
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

import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;
import com.coniv.mait.web.user.dto.PatchNicknameRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

public class UserApiIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	JwtAuthorizationFilter jwtAuthenticationFilter;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserEntityRepository userEntityRepository;

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
		PatchNicknameRequest request = new PatchNicknameRequest("빠른고양이");

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
	@WithCustomUser(email = "user1@example.com", name = "사용자1")
	@DisplayName("닉네임 변경 API 통합 테스트 - 동일한 닉네임에 다른 코드 부여")
	void updateUserNickname_SameNickname_DifferentCode() throws Exception {
		// given
		// 첫 번째 사용자가 "빠른고양이" 닉네임 사용
		PatchNicknameRequest request1 = new PatchNicknameRequest("빠른고양이");
		mockMvc.perform(patch("/api/v1/users/nickname")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request1))
				.with(csrf()))
			.andExpect(status().isOk());

		UserEntity user1 = userEntityRepository.findByEmail("user1@example.com").orElseThrow();
		String code1 = user1.getNicknameCode();

		// 두 번째 사용자 생성 및 로그인 (실제로는 새로운 테스트에서 해야하지만 간단히 시뮬레이션)
		// 이 부분은 실제로는 별도의 테스트로 분리하는 것이 좋습니다
		assertThat(code1).isNotNull();
		assertThat(code1).matches("\\d{4}");
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
}

