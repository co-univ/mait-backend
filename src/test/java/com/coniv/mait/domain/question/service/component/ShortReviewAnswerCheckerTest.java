package com.coniv.mait.domain.question.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.domain.question.service.dto.GradedAnswerShortResult;
import com.coniv.mait.domain.question.service.dto.ReviewAnswerCheckResult;
import com.coniv.mait.domain.solve.exception.QuestionSolvingException;
import com.coniv.mait.domain.solve.service.dto.ShortQuestionSubmitAnswer;

@ExtendWith(MockitoExtension.class)
class ShortReviewAnswerCheckerTest {

	@Mock
	private ShortAnswerEntityRepository shortAnswerEntityRepository;

	@InjectMocks
	private ShortReviewAnswerChecker checker;

	@Test
	@DisplayName("check - 정답과 제출 답안이 일치하면 isCorrect true")
	void check_correct() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(questionId);

		when(shortAnswerEntityRepository.findAllByShortQuestionId(questionId))
			.thenReturn(List.of(
				ShortAnswerEntity.builder().number(1L).answer("정답").build()
			));

		ShortQuestionSubmitAnswer submitAnswer = new ShortQuestionSubmitAnswer(List.of("정답"));

		// when
		ReviewAnswerCheckResult result = checker.check(questionId, question, submitAnswer);

		// then
		assertThat(result.questionId()).isEqualTo(questionId);
		assertThat(result.isCorrect()).isTrue();
		assertThat(result.type()).isEqualTo(QuestionType.SHORT);
		assertThat(result.gradedResults()).hasSize(1);

		GradedAnswerShortResult item = (GradedAnswerShortResult)result.gradedResults().get(0);
		assertThat(item.answer()).isEqualTo("정답");
		assertThat(item.isCorrect()).isTrue();
	}

	@Test
	@DisplayName("check - 오답 제출 시 isCorrect false")
	void check_wrong() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(questionId);

		when(shortAnswerEntityRepository.findAllByShortQuestionId(questionId))
			.thenReturn(List.of(
				ShortAnswerEntity.builder().number(1L).answer("정답").build()
			));

		ShortQuestionSubmitAnswer submitAnswer = new ShortQuestionSubmitAnswer(List.of("오답"));

		// when
		ReviewAnswerCheckResult result = checker.check(questionId, question, submitAnswer);

		// then
		assertThat(result.isCorrect()).isFalse();
		assertThat(result.gradedResults()).hasSize(1);

		GradedAnswerShortResult item = (GradedAnswerShortResult)result.gradedResults().get(0);
		assertThat(item.answer()).isEqualTo("오답");
		assertThat(item.isCorrect()).isFalse();
	}

	@Test
	@DisplayName("check - 여러 정답 중 하나와 일치하면 해당 item은 정답")
	void check_multipleCorrectAnswers() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(questionId);

		// 같은 number에 여러 정답 허용
		when(shortAnswerEntityRepository.findAllByShortQuestionId(questionId))
			.thenReturn(List.of(
				ShortAnswerEntity.builder().number(1L).answer("정답1").build(),
				ShortAnswerEntity.builder().number(1L).answer("정답2").build()
			));

		ShortQuestionSubmitAnswer submitAnswer = new ShortQuestionSubmitAnswer(List.of("정답2"));

		// when
		ReviewAnswerCheckResult result = checker.check(questionId, question, submitAnswer);

		// then
		assertThat(result.isCorrect()).isTrue();
	}

	@Test
	@DisplayName("check - 앞뒤 공백 제거 후 비교")
	void check_withSpaces() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(questionId);

		when(shortAnswerEntityRepository.findAllByShortQuestionId(questionId))
			.thenReturn(List.of(
				ShortAnswerEntity.builder().number(1L).answer("  정답  ").build()
			));

		ShortQuestionSubmitAnswer submitAnswer = new ShortQuestionSubmitAnswer(List.of("정답"));

		// when
		ReviewAnswerCheckResult result = checker.check(questionId, question, submitAnswer);

		// then
		assertThat(result.isCorrect()).isTrue();
	}

	@Test
	@DisplayName("check - 제출 개수가 정답 개수와 다르면 예외 발생")
	void check_answerCountMismatch() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(questionId);

		when(shortAnswerEntityRepository.findAllByShortQuestionId(questionId))
			.thenReturn(List.of(
				ShortAnswerEntity.builder().number(1L).answer("정답1").build(),
				ShortAnswerEntity.builder().number(2L).answer("정답2").build()
			));

		// 2개 정답인데 1개만 제출
		ShortQuestionSubmitAnswer submitAnswer = new ShortQuestionSubmitAnswer(List.of("정답1"));

		// when & then
		assertThatThrownBy(() -> checker.check(questionId, question, submitAnswer))
			.isInstanceOf(QuestionSolvingException.class);
	}
}
