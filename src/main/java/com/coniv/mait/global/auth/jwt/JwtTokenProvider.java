package com.coniv.mait.global.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import com.coniv.mait.global.auth.jwt.constant.TokenType;
import com.coniv.mait.global.auth.jwt.repository.BlackListRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

	private static final String USER_ID_CLAIM = "id";
	private static final String TOKEN_TYPE_CLAIM = "type";

	@Value("${jwt.secretKey}")
	String secretKey;

	@Value("${jwt.access.expiration}")
	Long accessExpiration;

	@Value("${jwt.refresh.expiration}")
	Long refreshExpiration;

	private final BlackListRepository blackListRepository;

	public Token createToken(final Long userId) {
		return Token.builder()
			.accessToken(generateToken(userId, TokenType.ACCESS, accessExpiration))
			.refreshToken(generateToken(userId, TokenType.REFRESH, refreshExpiration))
			.build();
	}

	private String generateToken(final Long userId, final TokenType tokenType, final long expireTime) {
		Claims claims = Jwts.claims();
		claims.put(USER_ID_CLAIM, userId);
		claims.put(TOKEN_TYPE_CLAIM, tokenType.name());

		SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

		return Jwts.builder()
			.setClaims(claims)
			.setIssuedAt(new Date())
			.setExpiration(new Date(System.currentTimeMillis() + expireTime))
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();
	}

	public void validateAccessToken(final String accessToken) {
		if (accessToken == null) {
			throw new BadCredentialsException("Access token is null");
		}
		if (!TokenType.ACCESS.name().equals(getTokenType(accessToken))) {
			throw new BadCredentialsException("Type is not access token");
		}
		if (blackListRepository.existsById(accessToken)) {
			throw new BadCredentialsException("Token is blacklisted");
		}
	}

	public void validateRefreshToken(final String refreshToken) {
		if (refreshToken == null) {
			throw new BadCredentialsException("Refresh token is null");
		}
		// 타입 클레임 도입 전에 발급된 refresh token은 reissue 시 저장된 토큰과의 일치 여부로 검증한다
		String tokenType = getTokenType(refreshToken);
		if (tokenType != null && !TokenType.REFRESH.name().equals(tokenType)) {
			throw new BadCredentialsException("Type is not refresh token");
		}
		if (blackListRepository.existsById(refreshToken)) {
			throw new BadCredentialsException("Refresh token is blacklisted");
		}
	}

	public Long getUserId(String token) {
		return parseClaims(token).get(USER_ID_CLAIM, Long.class);
	}

	private String getTokenType(final String token) {
		return parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class);
	}

	private Claims parseClaims(final String token) {
		return Jwts.parserBuilder()
			.setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
			.build()
			.parseClaimsJws(token)
			.getBody();
	}
}
