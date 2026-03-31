package com.coniv.mait.domain.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import com.coniv.mait.global.auth.jwt.BlackList;
import com.coniv.mait.global.auth.jwt.JwtTokenProvider;
import com.coniv.mait.global.auth.jwt.RefreshToken;
import com.coniv.mait.global.auth.jwt.Token;
import com.coniv.mait.global.auth.jwt.repository.BlackListRepository;
import com.coniv.mait.global.auth.jwt.repository.RefreshTokenRepository;
import com.coniv.mait.global.exception.custom.LoginFailException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@InjectMocks
	private AuthService authService;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private BlackListRepository blackListRepository;

	@Test
	@DisplayName("유효한 refresh token이면 새 토큰을 반환하고 기존 토큰을 블랙리스트에 등록한다")
	void reissue_Success() {
		// given
		String refreshToken = "valid.refresh.token";
		Long userId = 1L;
		Token newToken = Token.builder()
			.accessToken("new.access.token")
			.refreshToken("new.refresh.token")
			.build();

		willDoNothing().given(jwtTokenProvider).validateRefreshToken(refreshToken);
		given(jwtTokenProvider.getUserId(refreshToken)).willReturn(userId);
		given(refreshTokenRepository.findById(userId))
			.willReturn(Optional.of(new RefreshToken(userId, refreshToken)));
		given(jwtTokenProvider.createToken(userId)).willReturn(newToken);

		// when
		Token result = authService.reissue(refreshToken);

		// then
		assertThat(result.accessToken()).isEqualTo("new.access.token");
		assertThat(result.refreshToken()).isEqualTo("new.refresh.token");

		ArgumentCaptor<BlackList> blackListCaptor = ArgumentCaptor.forClass(BlackList.class);
		then(blackListRepository).should().save(blackListCaptor.capture());
		assertThat(blackListCaptor.getValue().getId()).isEqualTo(refreshToken);

		ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
		then(refreshTokenRepository).should().save(refreshTokenCaptor.capture());
		assertThat(refreshTokenCaptor.getValue().getRefreshToken()).isEqualTo(newToken.refreshToken());
	}

	@Test
	@DisplayName("blacklist에 등록된 refresh token이면 예외를 던진다")
	void reissue_BlacklistedToken_ThrowsException() {
		// given
		String blacklistedToken = "blacklisted.refresh.token";
		willThrow(new BadCredentialsException("Refresh token is blacklisted"))
			.given(jwtTokenProvider).validateRefreshToken(blacklistedToken);

		// when & then
		assertThatThrownBy(() -> authService.reissue(blacklistedToken))
			.isInstanceOf(BadCredentialsException.class)
			.hasMessage("Refresh token is blacklisted");

		then(refreshTokenRepository).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("저장된 refresh token이 없으면 LoginFailException을 던진다")
	void reissue_NoStoredToken_ThrowsException() {
		// given
		String refreshToken = "valid.refresh.token";
		Long userId = 1L;

		willDoNothing().given(jwtTokenProvider).validateRefreshToken(refreshToken);
		given(jwtTokenProvider.getUserId(refreshToken)).willReturn(userId);
		given(refreshTokenRepository.findById(userId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> authService.reissue(refreshToken))
			.isInstanceOf(LoginFailException.class)
			.hasMessage("저장된 Refresh Token이 없습니다.");
	}

	@Test
	@DisplayName("저장된 refresh token과 요청 token이 다르면 LoginFailException을 던진다")
	void reissue_TokenMismatch_ThrowsException() {
		// given
		String requestToken = "request.refresh.token";
		String storedToken = "stored.different.token";
		Long userId = 1L;

		willDoNothing().given(jwtTokenProvider).validateRefreshToken(requestToken);
		given(jwtTokenProvider.getUserId(requestToken)).willReturn(userId);
		given(refreshTokenRepository.findById(userId))
			.willReturn(Optional.of(new RefreshToken(userId, storedToken)));

		// when & then
		assertThatThrownBy(() -> authService.reissue(requestToken))
			.isInstanceOf(LoginFailException.class)
			.hasMessage("Refresh Token이 일치하지 않습니다.");
	}
}
