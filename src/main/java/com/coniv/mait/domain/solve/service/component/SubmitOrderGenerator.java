package com.coniv.mait.domain.solve.service.component;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SubmitOrderGenerator {

	private static final String KEY_PREFIX = "$submit:order:questionId:";

	private final RedisTemplate<String, Object> redisTemplate;

	public Long generateSubmitOrder(final Long questionId) {
		final String key = getKey(questionId);
		return redisTemplate.opsForValue().increment(key);
	}

	private String getKey(final Long questionId) {
		return KEY_PREFIX + questionId;
	}
}
