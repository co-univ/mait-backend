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

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.service.dto.GradedAnswerMultipleResult;
import com.coniv.mait.domain.question.service.dto.ReviewAnswerCheckResult;
import com.coniv.mait.domain.solve.service.dto.MultipleQuestionSubmitAnswer;

@ExtendWith(MockitoExtension.class)
class MultipleReviewAnswerCheckerTest {

	@Mock
	private MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	@InjectMocks
	private MultipleReviewAnswerChecker checker;

	@Test
	@DisplayName("check - 정답과 제출 답안이 완전히 일치하면 isCorrect true")
	void check_allCorrect() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(questionId);

		when(multipleChoiceEntityRepository.findAllByQuestionId(questionId))
			.thenReturn(List.of(
				MultipleChoiceEntity.builder().number(1).isCorrect(true).build(),
				MultipleChoiceEntity.builder().number(2).isCorrect(true).build(),
				MultipleChoiceEntity.builder().number(3).isCorrect(false).build()
			));

		MultipleQuestionSubmitAnswer submitAnswer = new MultipleQuestionSubmitAnswer(List.of(1L, 2L));

		// when
		ReviewAnswerCheckResult result = checker.check(questionId, question, submitAnswer);

		// then
		assertThat(result.questionId()).isEqualTo(questionId);
		assertThat(result.isCorrect()).isTrue();
		assertThat(result.type()).isEqualTo(QuestionType.MULTIPLE);
		assertThat(result.gradedResults()).hasSize(2);
	}

	@Test
	@DisplayName("check - 오답 선택 시 isCorrect false, 해당 item도 false")
	void check_wrongAnswer() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(questionId);

		when(multipleChoiceEntityRepository.findAllByQuestionId(questionId))
			.thenReturn(List.of(
				MultipleChoiceEntity.builder().number(1).isCorrect(true).build(),
				MultipleChoiceEntity.builder().number(2).isCorrect(false).build(),
				MultipleChoiceEntity.builder().number(3).isCorrect(false).build()
			));

		// 정답은 1번인데 2번을 제출
		MultipleQuestionSubmitAnswer submitAnswer = new MultipleQuestionSubmitAnswer(List.of(2L));

		// when
		ReviewAnswerCheckResult result = checker.check(questionId, question, submitAnswer);

		// then
		assertThat(result.isCorrect()).isFalse();
		assertThat(result.gradedResults()).hasSize(1);

		GradedAnswerMultipleResult item = (GradedAnswerMultipleResult)result.gradedResults().get(0);
		assertThat(item.number()).isEqualTo(2L);
		assertThat(item.isCorrect()).isFalse();
	}

	@Test
	@DisplayName("check - 정답 일부만 선택하면 isCorrect false")
	void check_partialCorrect() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(questionId);

		when(multipleChoiceEntityRepository.findAllByQuestionId(questionId))
			.thenReturn(List.of(
				MultipleChoiceEntity.builder().number(1).isCorrect(true).build(),
				MultipleChoiceEntity.builder().number(2).isCorrect(true).build(),
				MultipleChoiceEntity.builder().number(3).isCorrect(false).build()
			));

		// 정답은 1,2번인데 1번만 제출
		MultipleQuestionSubmitAnswer submitAnswer = new MultipleQuestionSubmitAnswer(List.of(1L));

		// when
		ReviewAnswerCheckResult result = checker.check(questionId, question, submitAnswer);

		// then
		assertThat(result.isCorrect()).isFalse();
		assertThat(result.gradedResults()).hasSize(1);

		GradedAnswerMultipleResult item = (GradedAnswerMultipleResult)result.gradedResults().get(0);
		assertThat(item.number()).isEqualTo(1L);
		assertThat(item.isCorrect()).isTrue(); // 1번 자체는 정답
	}

	@Test
	@DisplayName("check - 정답 + 오답 함께 선택하면 isCorrect false")
	void check_correctWithWrong() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(questionId);

		when(multipleChoiceEntityRepository.findAllByQuestionId(questionId))
			.thenReturn(List.of(
				MultipleChoiceEntity.builder().number(1).isCorrect(true).build(),
				MultipleChoiceEntity.builder().number(2).isCorrect(false).build()
			));

		// 정답 1번과 오답 2번 함께 제출
		MultipleQuestionSubmitAnswer submitAnswer = new MultipleQuestionSubmitAnswer(List.of(1L, 2L));

		// when
		ReviewAnswerCheckResult result = checker.check(questionId, question, submitAnswer);

		// then
		assertThat(result.isCorrect()).isFalse();
		assertThat(result.gradedResults()).hasSize(2);
	}
}
