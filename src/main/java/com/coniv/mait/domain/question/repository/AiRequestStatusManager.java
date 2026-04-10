package com.coniv.mait.domain.question.repository;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.enums.AiRequestStatus;
import com.coniv.mait.domain.question.service.component.QuestionRedisKeys;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AiRequestStatusManager {

	private static final long STATUS_TTL_MINUTES = 60;

	private final RedisTemplate<String, String> redisTemplate;

	public void updateStatus(Long questionSetId, AiRequestStatus status) {
		String key = QuestionRedisKeys.aiStatus(questionSetId);
		redisTemplate.opsForValue().set(key, status.name(), STATUS_TTL_MINUTES, TimeUnit.MINUTES);
	}

	public AiRequestStatus getStatus(Long questionSetId) {
		String key = QuestionRedisKeys.aiStatus(questionSetId);
		String value = redisTemplate.opsForValue().get(key);
		if (value == null) {
			return AiRequestStatus.NOT_FOUND;
		}
		return AiRequestStatus.valueOf(value);
	}
}
