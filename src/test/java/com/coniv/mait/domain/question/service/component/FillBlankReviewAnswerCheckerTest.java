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

import com.coniv.mait.domain.question.entity.FillBlankAnswerEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.FillBlankAnswerEntityRepository;
import com.coniv.mait.domain.question.service.dto.GradedAnswerFillBlankResult;
import com.coniv.mait.domain.question.service.dto.ReviewAnswerCheckResult;
import com.coniv.mait.domain.solve.exception.QuestionSolvingException;
import com.coniv.mait.domain.solve.service.dto.FillBlankQuestionSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.FillBlankSubmitAnswer;

@ExtendWith(MockitoExtension.class)
class FillBlankReviewAnswerCheckerTest {

	@Mock
	private FillBlankAnswerEntityRepository fillBlankAnswerEntityRepository;

	@InjectMocks
	private FillBlankReviewAnswerChecker checker;

	@Test
	@DisplayName("check - 모든 빈칸 정답 시 isCorrect true")
	void check_allCorrect() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(questionId);

		when(fillBlankAnswerEntityRepository.findAllByFillBlankQuestionId(questionId))
			.thenReturn(List.of(
				FillBlankAnswerEntity.builder().number(1L).answer("첫번째").build(),
				FillBlankAnswerEntity.builder().number(2L).answer("두번째").build()
			));

		FillBlankQuestionSubmitAnswer submitAnswer = new FillBlankQuestionSubmitAnswer(List.of(
			new FillBlankSubmitAnswer(1L, "첫번째"),
			new FillBlankSubmitAnswer(2L, "두번째")
		));

		// when
		ReviewAnswerCheckResult result = checker.check(questionId, question, submitAnswer);

		// then
		assertThat(result.questionId()).isEqualTo(questionId);
		assertThat(result.isCorrect()).isTrue();
		assertThat(result.type()).isEqualTo(QuestionType.FILL_BLANK);
		assertThat(result.gradedResults()).hasSize(2);
	}

	@Test
	@DisplayName("check - 일부 빈칸 오답 시 isCorrect false")
	void check_partialWrong() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(questionId);

		when(fillBlankAnswerEntityRepository.findAllByFillBlankQuestionId(questionId))
			.thenReturn(List.of(
				FillBlankAnswerEntity.builder().number(1L).answer("첫번째").build(),
				FillBlankAnswerEntity.builder().number(2L).answer("두번째").build()
			));

		FillBlankQuestionSubmitAnswer submitAnswer = new FillBlankQuestionSubmitAnswer(List.of(
			new FillBlankSubmitAnswer(1L, "첫번째"),
			new FillBlankSubmitAnswer(2L, "오답")
		));

		// when
		ReviewAnswerCheckResult result = checker.check(questionId, question, submitAnswer);

		// then
		assertThat(result.isCorrect()).isFalse();
		assertThat(result.gradedResults()).hasSize(2);

		GradedAnswerFillBlankResult item1 = (GradedAnswerFillBlankResult)result.gradedResults().get(0);
		GradedAnswerFillBlankResult item2 = (GradedAnswerFillBlankResult)result.gradedResults().get(1);

		assertThat(item1.isCorrect()).isTrue();
		assertThat(item2.isCorrect()).isFalse();
	}

	@Test
	@DisplayName("check - 같은 빈칸에 여러 정답 허용")
	void check_multipleAnswersPerBlank() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(questionId);

		when(fillBlankAnswerEntityRepository.findAllByFillBlankQuestionId(questionId))
			.thenReturn(List.of(
				FillBlankAnswerEntity.builder().number(1L).answer("정답1").isMain(true).build(),
				FillBlankAnswerEntity.builder().number(1L).answer("정답2").isMain(false).build()
			));

		FillBlankQuestionSubmitAnswer submitAnswer = new FillBlankQuestionSubmitAnswer(List.of(
			new FillBlankSubmitAnswer(1L, "정답2")
		));

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

		when(fillBlankAnswerEntityRepository.findAllByFillBlankQuestionId(questionId))
			.thenReturn(List.of(
				FillBlankAnswerEntity.builder().number(1L).answer("  정답  ").build()
			));

		FillBlankQuestionSubmitAnswer submitAnswer = new FillBlankQuestionSubmitAnswer(List.of(
			new FillBlankSubmitAnswer(1L, "정답")
		));

		// when
		ReviewAnswerCheckResult result = checker.check(questionId, question, submitAnswer);

		// then
		assertThat(result.isCorrect()).isTrue();
	}

	@Test
	@DisplayName("check - 중복된 빈칸 번호 제출 시 예외 발생")
	void check_duplicateNumber() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);

		FillBlankQuestionSubmitAnswer submitAnswer = new FillBlankQuestionSubmitAnswer(List.of(
			new FillBlankSubmitAnswer(1L, "답1"),
			new FillBlankSubmitAnswer(1L, "답2")  // 중복
		));

		// when & then
		assertThatThrownBy(() -> checker.check(questionId, question, submitAnswer))
			.isInstanceOf(QuestionSolvingException.class);
	}

	@Test
	@DisplayName("check - 존재하지 않는 빈칸 번호 제출 시 오답 처리")
	void check_nonExistentNumber() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(questionId);

		when(fillBlankAnswerEntityRepository.findAllByFillBlankQuestionId(questionId))
			.thenReturn(List.of(
				FillBlankAnswerEntity.builder().number(1L).answer("정답").build()
			));

		// 존재하지 않는 99번 빈칸에 제출
		FillBlankQuestionSubmitAnswer submitAnswer = new FillBlankQuestionSubmitAnswer(List.of(
			new FillBlankSubmitAnswer(99L, "답")
		));

		// when
		ReviewAnswerCheckResult result = checker.check(questionId, question, submitAnswer);

		// then
		assertThat(result.isCorrect()).isFalse();

		GradedAnswerFillBlankResult item = (GradedAnswerFillBlankResult)result.gradedResults().get(0);
		assertThat(item.number()).isEqualTo(99L);
		assertThat(item.isCorrect()).isFalse();
	}
}
