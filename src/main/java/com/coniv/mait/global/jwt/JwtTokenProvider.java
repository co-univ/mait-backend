package com.coniv.mait.global.jwt;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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

	public Token createToken(final Long userId) {
		return Token.builder()
			.accessToken(generateToken(userId, accessExpiration))
			.refreshToken(generateToken(userId, refreshExpiration))
			.build();
	}

	private String generateToken(final Long id, final long expireTime) {
		Date now = new Date(System.currentTimeMillis());
		Date expireDate = new Date(now.getTime() + expireTime);
		return Jwts.builder()
			.setSubject(String.valueOf(id))
			.setIssuedAt(now)
			.setExpiration(expireDate)
			.signWith(SignatureAlgorithm.HS256, secretKey)
			.compact();
	}
}

