package com.coniv.mait.global.interceptor.idempotency;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class IdempotencyRedisRepository {

	private static final String KEY_PREFIX = "$Idempotency:v2:";
	private static final Duration PROCESSING_TTL = Duration.ofSeconds(60);
	private static final Duration RESPONSE_TTL = Duration.ofSeconds(60);

	// 실행 소유자의 값과 일치할 때만 완료 또는 삭제한다.
	private static final DefaultRedisScript<Long> UPDATE_SCRIPT = new DefaultRedisScript<>("""
		if redis.call('GET', KEYS[1]) ~= ARGV[1] then
			return 0
		end
		if ARGV[2] == '' then
			return redis.call('DEL', KEYS[1])
		end
		redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
		return 1
		""", Long.class);

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public boolean claim(String key, IdempotencyRecord processing) {
		return Boolean.TRUE.equals(redisTemplate.opsForValue()
			.setIfAbsent(KEY_PREFIX + key, serialize(processing), PROCESSING_TTL));
	}

	public IdempotencyRecord get(String key) {
		String value = redisTemplate.opsForValue().get(KEY_PREFIX + key);
		if (value == null) {
			return null;
		}
		try {
			return objectMapper.readValue(value, IdempotencyRecord.class);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("멱등성 상태를 읽을 수 없습니다.", exception);
		}
	}

	public boolean complete(String key, IdempotencyRecord processing, IdempotencyResponse response) {
		return update(key, processing, serialize(processing.completed(response)), RESPONSE_TTL);
	}

	public boolean release(String key, IdempotencyRecord processing) {
		return update(key, processing, "", PROCESSING_TTL);
	}

	private boolean update(String key, IdempotencyRecord processing, String value, Duration ttl) {
		return Long.valueOf(1).equals(redisTemplate.execute(UPDATE_SCRIPT, List.of(KEY_PREFIX + key),
			serialize(processing), value, String.valueOf(ttl.toMillis())));
	}

	private String serialize(IdempotencyRecord record) {
		try {
			return objectMapper.writeValueAsString(record);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("멱등성 상태를 저장할 수 없습니다.", exception);
		}
	}
}
