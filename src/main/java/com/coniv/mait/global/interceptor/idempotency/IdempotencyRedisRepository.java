package com.coniv.mait.global.interceptor.idempotency;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class IdempotencyRedisRepository {

	private static final String KEY_PREFIX = "$Idempotency:v2:";
	private static final Duration PROCESSING_TTL = Duration.ofSeconds(60);
	private static final Duration RESPONSE_TTL = Duration.ofSeconds(60);

	private static final DefaultRedisScript<Long> COMPLETE_SCRIPT = new DefaultRedisScript<>("""
		if redis.call('GET', KEYS[1]) ~= ARGV[1] then
			return 0
		end
		redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
		return 1
		""", Long.class);

	private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>("""
		if redis.call('GET', KEYS[1]) ~= ARGV[1] then
			return 0
		end
		return redis.call('DEL', KEYS[1])
		""", Long.class);

	private final RedisTemplate<String, Object> redisTemplate;
	private final ObjectMapper objectMapper;

	public boolean claim(String key, IdempotencyRecord processing) {
		return Boolean.TRUE.equals(redisTemplate.opsForValue()
			.setIfAbsent(KEY_PREFIX + key, processing, PROCESSING_TTL));
	}

	public IdempotencyRecord get(String key) {
		Object value = redisTemplate.opsForValue().get(KEY_PREFIX + key);
		if (value == null) {
			return null;
		}
		return objectMapper.convertValue(value, IdempotencyRecord.class);
	}

	public boolean complete(String key, IdempotencyRecord processing, IdempotencyResponse response) {
		return Long.valueOf(1).equals(redisTemplate.execute(COMPLETE_SCRIPT, List.of(KEY_PREFIX + key),
			processing, processing.completed(response), RESPONSE_TTL.toMillis()));
	}

	public boolean release(String key, IdempotencyRecord processing) {
		return Long.valueOf(1).equals(redisTemplate.execute(RELEASE_SCRIPT, List.of(KEY_PREFIX + key), processing));
	}
}
