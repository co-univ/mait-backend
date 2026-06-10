package com.coniv.mait.domain.solve.service.component;

import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.service.component.QuestionRedisKeys;
import com.coniv.mait.domain.solve.service.dto.SubmitTimingDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SubmitTimingProcessor {

	private static final RedisSerializer<String> ARGS_SERIALIZER = RedisSerializer.string();
	private static final RedisSerializer<SubmitTimingDto> RESULT_SERIALIZER = new SubmitTimingResultSerializer();

	private final StringRedisTemplate stringRedisTemplate;

	private final DefaultRedisScript<SubmitTimingDto> submitTimingLuaScript;

	public SubmitTimingDto process(final Long questionId) {
		return stringRedisTemplate.execute(
			submitTimingLuaScript,
			ARGS_SERIALIZER,
			RESULT_SERIALIZER,
			List.of(QuestionRedisKeys.submitOrder(questionId), QuestionRedisKeys.submitFirstTime(questionId))
		);
	}
}
