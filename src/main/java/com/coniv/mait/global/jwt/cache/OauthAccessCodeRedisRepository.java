package com.coniv.mait.global.jwt.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OauthAccessCodeRedisRepository {

	private static final String KEY_PREFIX = "$oauth_access_code:";
	private static final long CODE_EXPIRATION = 30;
	private final RedisTemplate<String, String> redisTemplate;

	public void save(String code, String accessToken) {
		redisTemplate.opsForValue().set(
			generateKey(code),
			accessToken,
			CODE_EXPIRATION,
			TimeUnit.SECONDS
		);
	}

	public String findByCode(String code) {
		String accessToken = redisTemplate.opsForValue().get(generateKey(code));
		deleteByCode(code);
		return accessToken;
	}

	public void deleteByCode(String code) {
		redisTemplate.delete(generateKey(code));
	}

	private String generateKey(String code) {
		return KEY_PREFIX + code;
	}
}
