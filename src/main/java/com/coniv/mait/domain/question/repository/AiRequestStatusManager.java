package com.coniv.mait.domain.question.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.enums.AiRequestStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AiRequestStatusManager {

	private static final String KEY_PREFIX = "question_set_ai_status:";

	private final RedisTemplate<String, String> redisTemplate;

	public void updateStatus(Long questionSetId, AiRequestStatus status) {
		String key = KEY_PREFIX + questionSetId;
		redisTemplate.opsForValue().set(key, status.name());
	}
}

