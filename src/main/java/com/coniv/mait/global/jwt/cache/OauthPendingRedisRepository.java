package com.coniv.mait.global.jwt.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OauthPendingRedisRepository {

	private static final String KEY_PREFIX = "$oauth_pending:";
	private static final long PENDING_EXPIRATION = 5 * 60;

	private final RedisTemplate<String, String> redisTemplate;

	public void save(String key, String json) {
		redisTemplate.opsForValue().set(generateKey(key), json, PENDING_EXPIRATION, TimeUnit.SECONDS);
	}

	public String findByKey(String key) {
		return redisTemplate.opsForValue().get(generateKey(key));
	}

	public void deleteByKey(String key) {
		redisTemplate.delete(generateKey(key));
	}

	private String generateKey(String key) {
		return KEY_PREFIX + key;
	}
}

