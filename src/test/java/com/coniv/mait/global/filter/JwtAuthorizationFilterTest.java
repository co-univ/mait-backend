package com.coniv.mait.global.filter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.enums.LoginProvider;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.auth.jwt.JwtTokenProvider;
import com.coniv.mait.global.auth.jwt.Token;
import com.coniv.mait.global.auth.jwt.repository.BlackListRepository;
import com.coniv.mait.global.auth.model.MaitUser;

@ExtendWith(MockitoExtension.class)
class JwtAuthorizationFilterTest {

	@Mock
	private BlackListRepository blackListRepository;

	@Mock
	private UserEntityRepository userEntityRepository;

	private JwtTokenProvider jwtTokenProvider;

	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@BeforeEach
	void setUp() {
		jwtTokenProvider = new JwtTokenProvider(blackListRepository);
		ReflectionTestUtils.setField(jwtTokenProvider, "secretKey",
			"test-secret-key-for-testing-purposes-only-minimum-256-bits-required");
		ReflectionTestUtils.setField(jwtTokenProvider, "accessExpiration", 3_600_000L);
		ReflectionTestUtils.setField(jwtTokenProvider, "refreshExpiration", 604_800_000L);
		jwtAuthorizationFilter = new JwtAuthorizationFilter(jwtTokenProvider, userEntityRepository);
	}

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("access token으로 요청하면 인증 정보를 설정한다")
	void doFilter_AccessToken_SetsAuthentication() throws Exception {
		// given
		Long userId = 1L;
		Token token = jwtTokenProvider.createToken(userId);
		UserEntity user = UserEntity.socialLoginUser("test@example.com", "테스트유저", "providerId",
			LoginProvider.GOOGLE);
		ReflectionTestUtils.setField(user, "id", userId);
		given(blackListRepository.existsById(token.accessToken())).willReturn(false);
		given(userEntityRepository.findById(userId)).willReturn(Optional.of(user));

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
		request.addHeader("Authorization", "Bearer " + token.accessToken());
		MockFilterChain filterChain = new MockFilterChain();

		// when
		jwtAuthorizationFilter.doFilter(request, new MockHttpServletResponse(), filterChain);

		// then
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		assertThat(authentication).isNotNull();
		assertThat(((MaitUser)authentication.getPrincipal()).id()).isEqualTo(userId);
		assertThat(filterChain.getRequest()).isSameAs(request);
	}

	@Test
	@DisplayName("refresh token으로 요청하면 인증 정보를 설정하지 않는다")
	void doFilter_RefreshToken_DoesNotSetAuthentication() throws Exception {
		// given
		Token token = jwtTokenProvider.createToken(1L);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
		request.addHeader("Authorization", "Bearer " + token.refreshToken());
		MockFilterChain filterChain = new MockFilterChain();

		// when
		jwtAuthorizationFilter.doFilter(request, new MockHttpServletResponse(), filterChain);

		// then
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		assertThat(filterChain.getRequest()).isSameAs(request);
		then(userEntityRepository).shouldHaveNoInteractions();
	}
}
