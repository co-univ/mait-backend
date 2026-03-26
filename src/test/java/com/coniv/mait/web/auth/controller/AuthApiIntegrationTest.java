package com.coniv.mait.web.auth.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.enums.LoginProvider;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.auth.jwt.JwtTokenProvider;
import com.coniv.mait.global.auth.jwt.RefreshToken;
import com.coniv.mait.global.auth.jwt.Token;
import com.coniv.mait.global.auth.jwt.constant.TokenConstants;
import com.coniv.mait.global.auth.jwt.repository.RefreshTokenRepository;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.web.integration.BaseIntegrationTest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;

public class AuthApiIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@AfterEach
	void clear() {
		userEntityRepository.deleteAll();
		refreshTokenRepository.deleteAll();
	}

	void passThroughJwtFilter() throws Exception {
		Mockito.doAnswer(inv -> {
			ServletRequest request = inv.getArgument(0);
			ServletResponse response = inv.getArgument(1);
			FilterChain chain = inv.getArgument(2);
			chain.doFilter(request, response);
			return null;
		}).when(jwtAuthorizationFilter).doFilter(Mockito.any(), Mockito.any(), Mockito.any());
	}

	@Test
	@DisplayName("유효한 refresh token으로 요청하면 새 토큰을 발급한다")
	void reissue_Success() throws Exception {
		// given
		passThroughJwtFilter();

		UserEntity user = userEntityRepository.save(
			UserEntity.socialLoginUser("test@example.com", "테스트유저", "providerId", LoginProvider.GOOGLE)
		);

		Token token = jwtTokenProvider.createToken(user.getId());
		refreshTokenRepository.save(new RefreshToken(user.getId(), token.refreshToken()));

		// when
		MvcResult result = mockMvc.perform(post("/api/v1/auth/reissue")
				.with(csrf())
				.cookie(new Cookie(TokenConstants.REFRESH_TOKEN, token.refreshToken())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andReturn();

		// then
		String newAccessToken = result.getResponse().getHeader("Authorization");
		String newRefreshCookie = result.getResponse().getHeader("Set-Cookie");

		assertThat(newAccessToken).isNotBlank();
		assertThat(newRefreshCookie).contains(TokenConstants.REFRESH_TOKEN);

		// 기존 refresh token이 새 토큰으로 교체됐는지 검증 (Redis에 저장된 토큰이 존재)
		assertThat(refreshTokenRepository.findById(user.getId())).isPresent();
	}
}
