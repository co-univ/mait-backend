package com.coniv.mait.web.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.enums.LoginProvider;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.auth.jwt.JwtTokenProvider;
import com.coniv.mait.global.auth.jwt.Token;
import com.coniv.mait.web.integration.BaseIntegrationTest;

public class TokenAuthenticationApiIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@AfterEach
	void clear() {
		userEntityRepository.deleteAll();
	}

	@Test
	@DisplayName("access token을 Bearer로 전달하면 인증이 필요한 API를 호출할 수 있다")
	void accessToken_Authenticated_Success() throws Exception {
		// given
		UserEntity user = userEntityRepository.save(
			UserEntity.socialLoginUser("test@example.com", "테스트유저", "providerId", LoginProvider.GOOGLE)
		);
		Token token = jwtTokenProvider.createToken(user.getId());

		// when & then
		mockMvc.perform(get("/api/v1/users/me")
				.header("Authorization", "Bearer " + token.accessToken()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data.id").value(user.getId()))
			.andExpect(jsonPath("$.data.email").value("test@example.com"));
	}
}
