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
	@DisplayName("check - 인정답안이 겹쳐도 일대일 배정이 가능하면 모두 정답")
	void check_reassignsOverlappingAnswer() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(questionId);
		when(shortAnswerEntityRepository.findAllByShortQuestionId(questionId)).thenReturn(List.of(
			ShortAnswerEntity.builder().number(10L).answer("alpha").build(),
			ShortAnswerEntity.builder().number(10L).answer("beta").build(),
			ShortAnswerEntity.builder().number(20L).answer("alpha").build()
		));
		ShortQuestionSubmitAnswer submitAnswer = new ShortQuestionSubmitAnswer(List.of("alpha", "beta"));

		// when
		ReviewAnswerCheckResult result = checker.check(questionId, question, submitAnswer);

		// then
		assertThat(result.isCorrect()).isTrue();
		assertThat(result.gradedResults()).isEqualTo(List.of(
			new GradedAnswerShortResult("alpha", true),
			new GradedAnswerShortResult("beta", true)
		));
	}

	@Test
	@DisplayName("check - 제출 순서가 바뀌어도 일대일 배정하여 정답 처리")
	void check_reversedOverlappingAnswers() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(questionId);
		when(shortAnswerEntityRepository.findAllByShortQuestionId(questionId)).thenReturn(List.of(
			ShortAnswerEntity.builder().number(1L).answer("alpha").build(),
			ShortAnswerEntity.builder().number(1L).answer("beta").build(),
			ShortAnswerEntity.builder().number(2L).answer("alpha").build()
		));
		ShortQuestionSubmitAnswer submitAnswer = new ShortQuestionSubmitAnswer(List.of("beta", "alpha"));

		// when
		ReviewAnswerCheckResult result = checker.check(questionId, question, submitAnswer);

		// then
		assertThat(result.isCorrect()).isTrue();
		assertThat(result.gradedResults()).isEqualTo(List.of(
			new GradedAnswerShortResult("beta", true),
			new GradedAnswerShortResult("alpha", true)
		));
	}

	@Test
	@DisplayName("check - 같은 문자열도 별도 제출이면 서로 다른 정답 그룹에 배정")
	void check_repeatedSubmissionMatchesDifferentGroups() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(questionId);
		when(shortAnswerEntityRepository.findAllByShortQuestionId(questionId)).thenReturn(List.of(
			ShortAnswerEntity.builder().number(1L).answer("alpha").build(),
			ShortAnswerEntity.builder().number(2L).answer("alpha").build()
		));
		ShortQuestionSubmitAnswer submitAnswer = new ShortQuestionSubmitAnswer(List.of("alpha", "alpha"));

		// when
		ReviewAnswerCheckResult result = checker.check(questionId, question, submitAnswer);

		// then
		assertThat(result.isCorrect()).isTrue();
		assertThat(result.gradedResults()).isEqualTo(List.of(
			new GradedAnswerShortResult("alpha", true),
			new GradedAnswerShortResult("alpha", true)
		));
	}

	@Test
	@DisplayName("check - 세 그룹에 걸쳐 기존 배정을 바꿔야 해도 모두 정답")
	void check_reassignsAcrossMultipleGroups() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(questionId);
		when(shortAnswerEntityRepository.findAllByShortQuestionId(questionId)).thenReturn(List.of(
			ShortAnswerEntity.builder().number(1L).answer("alpha").build(),
			ShortAnswerEntity.builder().number(1L).answer("beta").build(),
			ShortAnswerEntity.builder().number(2L).answer("alpha").build(),
			ShortAnswerEntity.builder().number(2L).answer("gamma").build(),
			ShortAnswerEntity.builder().number(3L).answer("alpha").build()
		));
		ShortQuestionSubmitAnswer submitAnswer = new ShortQuestionSubmitAnswer(List.of("alpha", "beta", "gamma"));

		// when
		ReviewAnswerCheckResult result = checker.check(questionId, question, submitAnswer);

		// then
		assertThat(result.isCorrect()).isTrue();
		assertThat(result.gradedResults()).isEqualTo(List.of(
			new GradedAnswerShortResult("alpha", true),
			new GradedAnswerShortResult("beta", true),
			new GradedAnswerShortResult("gamma", true)
		));
	}

	@Test
	@DisplayName("check - 오답이 섞여도 겹치는 인정답안을 재배정하여 맞은 답을 표시")
	void check_partialWrongPreservesReassignedResults() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(questionId);
		when(shortAnswerEntityRepository.findAllByShortQuestionId(questionId)).thenReturn(List.of(
			ShortAnswerEntity.builder().number(1L).answer("alpha").build(),
			ShortAnswerEntity.builder().number(1L).answer("beta").build(),
			ShortAnswerEntity.builder().number(2L).answer("alpha").build(),
			ShortAnswerEntity.builder().number(3L).answer("gamma").build()
		));
		ShortQuestionSubmitAnswer submitAnswer = new ShortQuestionSubmitAnswer(List.of("alpha", "beta", "wrong"));

		// when
		ReviewAnswerCheckResult result = checker.check(questionId, question, submitAnswer);

		// then
		assertThat(result.isCorrect()).isFalse();
		assertThat(result.gradedResults()).isEqualTo(List.of(
			new GradedAnswerShortResult("alpha", true),
			new GradedAnswerShortResult("beta", true),
			new GradedAnswerShortResult("wrong", false)
		));
	}

	@Test
	@DisplayName("check - 첫 답이 오답이어도 나머지 답의 일대일 배정을 계속 수행")
	void check_wrongFirstAnswerDoesNotPreventLaterMatches() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(questionId);
		when(shortAnswerEntityRepository.findAllByShortQuestionId(questionId)).thenReturn(List.of(
			ShortAnswerEntity.builder().number(1L).answer("alpha").build(),
			ShortAnswerEntity.builder().number(1L).answer("beta").build(),
			ShortAnswerEntity.builder().number(2L).answer("alpha").build(),
			ShortAnswerEntity.builder().number(3L).answer("gamma").build()
		));
		ShortQuestionSubmitAnswer submitAnswer = new ShortQuestionSubmitAnswer(List.of("wrong", "alpha", "beta"));

		// when
		ReviewAnswerCheckResult result = checker.check(questionId, question, submitAnswer);

		// then
		assertThat(result.isCorrect()).isFalse();
		assertThat(result.gradedResults()).isEqualTo(List.of(
			new GradedAnswerShortResult("wrong", false),
			new GradedAnswerShortResult("alpha", true),
			new GradedAnswerShortResult("beta", true)
		));
	}

	@Test
	@DisplayName("check - 같은 그룹에만 맞는 답이 여럿이면 먼저 제출한 답만 정답")
	void check_competingAnswersCannotReuseGroup() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(questionId);
		when(shortAnswerEntityRepository.findAllByShortQuestionId(questionId)).thenReturn(List.of(
			ShortAnswerEntity.builder().number(1L).answer("alpha").build(),
			ShortAnswerEntity.builder().number(1L).answer("beta").build(),
			ShortAnswerEntity.builder().number(2L).answer("gamma").build(),
			ShortAnswerEntity.builder().number(3L).answer("gamma").build()
		));
		ShortQuestionSubmitAnswer submitAnswer = new ShortQuestionSubmitAnswer(List.of("alpha", "beta", "gamma"));

		// when
		ReviewAnswerCheckResult result = checker.check(questionId, question, submitAnswer);

		// then
		assertThat(result.isCorrect()).isFalse();
		assertThat(result.gradedResults()).isEqualTo(List.of(
			new GradedAnswerShortResult("alpha", true),
			new GradedAnswerShortResult("beta", false),
			new GradedAnswerShortResult("gamma", true)
		));
	}

	@Test
	@DisplayName("check - 정규화한 값으로 배정하고 개별 결과에는 제출 원문을 유지")
	void check_normalizesOverlappingAnswersAndPreservesOriginalText() {
		// given
		Long questionId = 1L;
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(questionId);
		when(shortAnswerEntityRepository.findAllByShortQuestionId(questionId)).thenReturn(List.of(
			ShortAnswerEntity.builder().number(1L).answer(" ALPHA ").build(),
			ShortAnswerEntity.builder().number(1L).answer(" Beta ").build(),
			ShortAnswerEntity.builder().number(2L).answer(" alpha ").build()
		));
		ShortQuestionSubmitAnswer submitAnswer = new ShortQuestionSubmitAnswer(List.of(" alpha ", " BETA "));

		// when
		ReviewAnswerCheckResult result = checker.check(questionId, question, submitAnswer);

		// then
		assertThat(result.isCorrect()).isTrue();
		assertThat(result.gradedResults()).isEqualTo(List.of(
			new GradedAnswerShortResult(" alpha ", true),
			new GradedAnswerShortResult(" BETA ", true)
		));
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
