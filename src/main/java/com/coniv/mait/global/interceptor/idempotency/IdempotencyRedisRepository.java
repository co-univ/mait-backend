package com.coniv.mait.global.interceptor.idempotency;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class IdempotencyRedisRepository {

	private static final String KEY_PREFIX = "$Idempotency:";

	private static final String STATUS_KEY_PREFIX = KEY_PREFIX + "Status:";

	private static final String RESULT_KEY_PREFIX = KEY_PREFIX + "Result:";
	private static final int RESPONSE_EXPIRATION = 30;

	private final RedisTemplate<String, Object> redisTemplate;

	private String getStatusKey(final String idempotencyKey) {
		return KEY_PREFIX + idempotencyKey;
	}

	private String getResultKey(final String idempotencyKey) {
		return RESULT_KEY_PREFIX + idempotencyKey;
	}

	public void setProcessing(String idempotencyKey) {
		String key = getStatusKey(idempotencyKey);
		redisTemplate.opsForValue().set(key, IdempotencyStatus.PROCESSING, Duration.ofSeconds(RESPONSE_EXPIRATION));
	}

	public void setCompleted(String idempotencyKey, Object responseBody) {
		String statusKey = getStatusKey(idempotencyKey);
		String resultKey = getResultKey(idempotencyKey);

		redisTemplate.opsForValue()
			.set(statusKey, IdempotencyStatus.COMPLETED, Duration.ofSeconds(RESPONSE_EXPIRATION));

		redisTemplate.opsForValue().set(resultKey, responseBody, Duration.ofSeconds(RESPONSE_EXPIRATION));
	}

	public IdempotencyStatus getStatus(String idempotencyKey) {
		String statusKey = getStatusKey(idempotencyKey);
		Object status = redisTemplate.opsForValue().get(statusKey);
		if (status == null) {
			return null;
		}
		return IdempotencyStatus.valueOf((String)status);
	}

	public Object getResponse(String idempotencyKey) {
		return redisTemplate.opsForValue().get(getResultKey(idempotencyKey));
	}
}
