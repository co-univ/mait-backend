package com.coniv.mait.global.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import com.coniv.mait.global.auth.jwt.repository.BlackListRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

	@Value("${jwt.secretKey}")
	String secretKey;

	@Value("${jwt.access.expiration}")
	Long accessExpiration;

	@Value("${jwt.refresh.expiration}")
	Long refreshExpiration;

	private final BlackListRepository blackListRepository;

	public Token createToken(final Long userId) {
		return Token.builder()
			.accessToken(generateToken(userId, accessExpiration))
			.refreshToken(generateToken(userId, refreshExpiration))
			.build();
	}

	private String generateToken(final Long userId, final long expireTime) {
		Claims claims = Jwts.claims();
		claims.put("id", userId);

		SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

		return Jwts.builder()
			.setClaims(claims)
			.setIssuedAt(new Date())
			.setExpiration(new Date(System.currentTimeMillis() + expireTime))
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();
	}

	public void validateAccessToken(final String accessToken) {
		// Todo: 액세스 토큰 타입 검증
		if (accessToken == null) {
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
		if (blackListRepository.existsById(refreshToken)) {
			throw new BadCredentialsException("Refresh token is blacklisted");
		}
	}

	public Long getUserId(String token) {
		Claims claims = Jwts.parserBuilder()
			.setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
			.build()
			.parseClaimsJws(token)
			.getBody();

		return claims.get("id", Long.class);
	}
}
