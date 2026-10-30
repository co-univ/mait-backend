package com.coniv.mait.global.auth.jwt;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import com.coniv.mait.global.auth.jwt.repository.BlackListRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

	private static final String SECRET_KEY = "test-secret-key-for-testing-purposes-only-minimum-256-bits-required";

	@InjectMocks
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private BlackListRepository blackListRepository;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", SECRET_KEY);
		ReflectionTestUtils.setField(jwtTokenProvider, "accessExpiration", 3_600_000L);
		ReflectionTestUtils.setField(jwtTokenProvider, "refreshExpiration", 604_800_000L);
	}

	@Test
	@DisplayName("발급한 access token과 refresh token에 각각의 용도와 사용자 ID가 담긴다")
	void createToken_ContainsTokenType() {
		// when
		Token token = jwtTokenProvider.createToken(1L);

		// then
		assertThat(parseClaims(token.accessToken()).get("type", String.class)).isEqualTo("ACCESS");
		assertThat(parseClaims(token.refreshToken()).get("type", String.class)).isEqualTo("REFRESH");
		assertThat(jwtTokenProvider.getUserId(token.accessToken())).isEqualTo(1L);
		assertThat(jwtTokenProvider.getUserId(token.refreshToken())).isEqualTo(1L);
	}

	@Test
	@DisplayName("access token은 access token 검증을 통과한다")
	void validateAccessToken_Success() {
		// given
		Token token = jwtTokenProvider.createToken(1L);
		given(blackListRepository.existsById(token.accessToken())).willReturn(false);

		// when & then
		assertThatCode(() -> jwtTokenProvider.validateAccessToken(token.accessToken()))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("refresh token으로 access token 검증을 하면 예외를 던진다")
	void validateAccessToken_RefreshToken_ThrowsException() {
		// given
		Token token = jwtTokenProvider.createToken(1L);

		// when & then
		assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(token.refreshToken()))
			.isInstanceOf(BadCredentialsException.class)
			.hasMessage("Type is not access token");
		then(blackListRepository).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("타입 클레임이 없는 토큰으로 access token 검증을 하면 예외를 던진다")
	void validateAccessToken_NoTokenType_ThrowsException() {
		// given
		String legacyToken = generateTokenWithoutType(1L);

		// when & then
		assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(legacyToken))
			.isInstanceOf(BadCredentialsException.class)
			.hasMessage("Type is not access token");
	}

	@Test
	@DisplayName("null 토큰으로 access token 검증을 하면 예외를 던진다")
	void validateAccessToken_Null_ThrowsException() {
		// when & then
		assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(null))
			.isInstanceOf(BadCredentialsException.class)
			.hasMessage("Access token is null");
	}

	@Test
	@DisplayName("blacklist에 등록된 access token이면 예외를 던진다")
	void validateAccessToken_Blacklisted_ThrowsException() {
		// given
		Token token = jwtTokenProvider.createToken(1L);
		given(blackListRepository.existsById(token.accessToken())).willReturn(true);

		// when & then
		assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(token.accessToken()))
			.isInstanceOf(BadCredentialsException.class)
			.hasMessage("Token is blacklisted");
	}

	@Test
	@DisplayName("refresh token은 refresh token 검증을 통과한다")
	void validateRefreshToken_Success() {
		// given
		Token token = jwtTokenProvider.createToken(1L);
		given(blackListRepository.existsById(token.refreshToken())).willReturn(false);

		// when & then
		assertThatCode(() -> jwtTokenProvider.validateRefreshToken(token.refreshToken()))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("타입 클레임이 없는 토큰으로 refresh token 검증을 하면 예외를 던진다")
	void validateRefreshToken_NoTokenType_ThrowsException() {
		// given
		String legacyToken = generateTokenWithoutType(1L);

		// when & then
		assertThatThrownBy(() -> jwtTokenProvider.validateRefreshToken(legacyToken))
			.isInstanceOf(BadCredentialsException.class)
			.hasMessage("Type is not refresh token");
	}

	@Test
	@DisplayName("access token으로 refresh token 검증을 하면 예외를 던진다")
	void validateRefreshToken_AccessToken_ThrowsException() {
		// given
		Token token = jwtTokenProvider.createToken(1L);

		// when & then
		assertThatThrownBy(() -> jwtTokenProvider.validateRefreshToken(token.accessToken()))
			.isInstanceOf(BadCredentialsException.class)
			.hasMessage("Type is not refresh token");
		then(blackListRepository).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("blacklist에 등록된 refresh token이면 예외를 던진다")
	void validateRefreshToken_Blacklisted_ThrowsException() {
		// given
		Token token = jwtTokenProvider.createToken(1L);
		given(blackListRepository.existsById(token.refreshToken())).willReturn(true);

		// when & then
		assertThatThrownBy(() -> jwtTokenProvider.validateRefreshToken(token.refreshToken()))
			.isInstanceOf(BadCredentialsException.class)
			.hasMessage("Refresh token is blacklisted");
	}

	private Claims parseClaims(String token) {
		return Jwts.parserBuilder()
			.setSigningKey(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)))
			.build()
			.parseClaimsJws(token)
			.getBody();
	}

	private String generateTokenWithoutType(Long userId) {
		Claims claims = Jwts.claims();
		claims.put("id", userId);
		return Jwts.builder()
			.setClaims(claims)
			.setIssuedAt(new Date())
			.setExpiration(new Date(System.currentTimeMillis() + 3_600_000L))
			.signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
			.compact();
	}
}
