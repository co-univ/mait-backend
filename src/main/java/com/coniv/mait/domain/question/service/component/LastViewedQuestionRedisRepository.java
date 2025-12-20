package com.coniv.mait.domain.question.service.component;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LastViewedQuestionRedisRepository {

	private static final String LAST_VIEW_QUESTION_PREFIX = "review:last_viewed:question_set:";
	private static final String USER_ID = "user:";
	private static final long CACHE_HOURS = 24;

	private final QuestionReader questionReader;

	private final RedisTemplate<String, String> redisTemplate;

	public QuestionEntity getLastViewedQuestion(QuestionSetEntity questionSet, Long userId) {
		final String key = getKey(questionSet, userId);
		String redisValue = redisTemplate.opsForValue().get(key);

		if (redisValue == null) {
			return questionReader.getFirstQuestion(questionSet);
		}

		return questionReader.getQuestion(Long.parseLong(redisValue));
	}

	public void updateLastViewedQuestion(QuestionSetEntity questionSet, QuestionEntity question, Long userId) {
		final String key = getKey(questionSet, userId);

		redisTemplate.opsForValue().set(key, String.valueOf(question.getId()), CACHE_HOURS, TimeUnit.HOURS);
	}

	private String getKey(final QuestionSetEntity questionSet, final Long userId) {
		return LAST_VIEW_QUESTION_PREFIX + questionSet.getId() + USER_ID + userId;
	}
}
