package com.coniv.mait.domain.solve.service.component;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.service.component.QuestionRedisKeys;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SubmitOrderGenerator {

	private final RedisTemplate<String, Object> redisTemplate;

	public Long generateSubmitOrder(final Long questionId) {
		final String key = QuestionRedisKeys.submitOrder(questionId);
		return redisTemplate.opsForValue().increment(key);
	}
}
