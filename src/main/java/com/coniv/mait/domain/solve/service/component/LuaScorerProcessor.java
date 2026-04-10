package com.coniv.mait.domain.solve.service.component;

import java.util.Collections;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.service.component.QuestionRedisKeys;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LuaScorerProcessor implements ScorerProcessor {

	private final StringRedisTemplate stringRedisTemplate;

	private final DefaultRedisScript<String> winnerLuaScript;

	@Override
	public Long getScorer(Long questionId, Long userId, Long submitOrder) {
		if (submitOrder == null) {
			throw new IllegalArgumentException("submitOrder cannot be null");
		}
		String key = QuestionRedisKeys.scorer(questionId);

		String result = stringRedisTemplate.execute(
			winnerLuaScript, Collections.singletonList(key),
			String.valueOf(userId), String.valueOf(submitOrder)
		);

		return Long.parseLong(result);
	}
}
