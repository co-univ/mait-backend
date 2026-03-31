package com.coniv.mait.web.auth.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.coniv.mait.domain.auth.service.AuthService;
import com.coniv.mait.global.auth.cookie.CookieFactory;
import com.coniv.mait.global.auth.jwt.Token;
import com.coniv.mait.global.auth.jwt.constant.TokenConstants;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.web.integration.BaseIntegrationTest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;

public class AuthControllerTest extends BaseIntegrationTest {

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private CookieFactory cookieFactory;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@BeforeEach
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
	@DisplayName("유효한 refresh token 쿠키로 요청하면 새 토큰을 응답한다")
	void reissue_Success() throws Exception {
		// given
		String refreshToken = "valid.refresh.token";
		Token newToken = Token.builder()
			.accessToken("new.access.token")
			.refreshToken("new.refresh.token")
			.build();

		BDDMockito.given(authService.reissue(refreshToken)).willReturn(newToken);
		BDDMockito.given(cookieFactory.createRefreshResponseCookie(newToken.refreshToken()))
			.willReturn(ResponseCookie.from(TokenConstants.REFRESH_TOKEN, newToken.refreshToken()).build());

		// when & then
		mockMvc.perform(post("/api/v1/auth/reissue")
				.with(csrf())
				.cookie(new Cookie(TokenConstants.REFRESH_TOKEN, refreshToken)))
			.andExpect(status().isOk())
			.andExpect(header().string("Authorization", newToken.accessToken()))
			.andExpect(jsonPath("$.isSuccess").value(true));
	}
}
