package com.coniv.mait.domain.solve.service.component;

import java.util.Collections;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LuaScorerProcessor implements ScorerProcessor {

	private static final String SCORER_KEY_PREFIX = "$scorer:questionId:";

	private final RedisTemplate<String, Object> redisTemplate;

	private final DefaultRedisScript<String> winnerLuaScript;

	@Override
	public Long getScorer(Long questionId, Long userId, Long submitOrder) {
		String key = getKey(questionId);
		String result = redisTemplate.execute(
			winnerLuaScript, Collections.singletonList(key),
			userId, String.valueOf(submitOrder)
		);
		return Long.parseLong(result);
	}

	private String getKey(final Long questionId) {
		return SCORER_KEY_PREFIX + questionId;
	}
}
