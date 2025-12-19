package com.coniv.mait.domain.question.service.component;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;

@ExtendWith(MockitoExtension.class)
class LastViewedQuestionRedisRepositoryTest {

	@InjectMocks
	private LastViewedQuestionRedisRepository lastViewedQuestionRedisRepository;

	@Mock
	private QuestionReader questionReader;

	@Mock
	private RedisTemplate<String, String> redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Test
	@DisplayName("저장된 문제가 없을 시 가장 첫번째 문제 반환")
	void getLastViewedQuestion_null() {
		// given
		final Long userId = 1L;
		final Long questionSetId = 1L;
		final String key = "review:last_viewed:question_set:" + questionSetId + "user:" + userId;
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSet.getId()).thenReturn(questionSetId);

		QuestionEntity firstQuestion = mock(QuestionEntity.class);

		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(key)).thenReturn(null);
		when(questionReader.getFirstQuestion(questionSet)).thenReturn(firstQuestion);
		// when
		QuestionEntity lastViewedQuestion = lastViewedQuestionRedisRepository.getLastViewedQuestion(questionSet,
			userId);

		// then
		assertSame(firstQuestion, lastViewedQuestion);
		verify(questionReader).getFirstQuestion(questionSet);
		verify(questionReader, never()).getQuestion(anyLong());
	}

	@Test
	@DisplayName("저장된 문제가 있으면 해당 문제 반환")
	void getLastViewedQuestion_not_null() {
		// given
		final Long userId = 1L;
		final Long questionSetId = 2L;
		final Long questionId = 3L;
		final String key = "review:last_viewed:question_set:" + questionSetId + "user:" + userId;
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSet.getId()).thenReturn(questionSetId);

		QuestionEntity lastViewedQuestion = mock(QuestionEntity.class);

		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(key)).thenReturn(String.valueOf(questionId));
		when(questionReader.getQuestion(questionId)).thenReturn(lastViewedQuestion);

		// when
		QuestionEntity savedQuestion = lastViewedQuestionRedisRepository.getLastViewedQuestion(questionSet,
			userId);

		// then
		assertSame(lastViewedQuestion, savedQuestion);
		verify(questionReader).getQuestion(questionId);
		verify(questionReader, never()).getFirstQuestion(any(QuestionSetEntity.class));
	}
}