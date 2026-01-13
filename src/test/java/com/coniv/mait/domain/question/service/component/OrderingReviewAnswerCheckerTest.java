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
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.service.dto.ReviewAnswerCheckResult;
import com.coniv.mait.domain.solve.service.component.AnswerGrader;
import com.coniv.mait.domain.solve.service.dto.OrderingQuestionSubmitAnswer;

@ExtendWith(MockitoExtension.class)
class OrderingReviewAnswerCheckerTest {

	@Mock
	private AnswerGrader answerGrader;

	@InjectMocks
	private OrderingReviewAnswerChecker checker;

	@Test
	@DisplayName("check - 정답이면 isCorrect true, items는 null")
	void check_correct() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);

		OrderingQuestionSubmitAnswer submitAnswer = new OrderingQuestionSubmitAnswer(List.of(1L, 2L, 3L));

		when(answerGrader.gradeAnswer(question, submitAnswer)).thenReturn(true);

		// when
		ReviewAnswerCheckResult result = checker.check(questionId, question, submitAnswer);

		// then
		assertThat(result.questionId()).isEqualTo(questionId);
		assertThat(result.isCorrect()).isTrue();
		assertThat(result.type()).isEqualTo(QuestionType.ORDERING);
		assertThat(result.gradedResults()).isEmpty(); // 순서유형은 피드백 없음
	}

	@Test
	@DisplayName("check - 오답이면 isCorrect false, items는 null")
	void check_wrong() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);

		OrderingQuestionSubmitAnswer submitAnswer = new OrderingQuestionSubmitAnswer(List.of(3L, 2L, 1L));

		when(answerGrader.gradeAnswer(question, submitAnswer)).thenReturn(false);

		// when
		ReviewAnswerCheckResult result = checker.check(questionId, question, submitAnswer);

		// then
		assertThat(result.questionId()).isEqualTo(questionId);
		assertThat(result.isCorrect()).isFalse();
		assertThat(result.type()).isEqualTo(QuestionType.ORDERING);
		assertThat(result.gradedResults()).isEmpty(); // 순서유형은 오답이어도 피드백 없음
	}

	@Test
	@DisplayName("check - AnswerGrader에 채점 위임")
	void check_delegatesToAnswerGrader() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);
		OrderingQuestionSubmitAnswer submitAnswer = new OrderingQuestionSubmitAnswer(List.of(1L, 2L));

		when(answerGrader.gradeAnswer(question, submitAnswer)).thenReturn(true);

		// when
		checker.check(questionId, question, submitAnswer);

		// then
		verify(answerGrader).gradeAnswer(question, submitAnswer);
	}
}
