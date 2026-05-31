package com.coniv.mait.domain.statistic.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.service.component.AnswerSubmitRecordReader;

@ExtendWith(MockitoExtension.class)
class QuestionSetStatisticCalculatorTest {

	private static final Long USER_ID = 1L;

	@InjectMocks
	private QuestionSetStatisticCalculator questionSetStatisticCalculator;

	@Mock
	private AnswerSubmitRecordReader answerSubmitRecordReader;

	@Test
	@DisplayName("전체 정답률 - 모든 문제의 첫 제출을 통째로 모아 (정답 수 / 전체 수)로 가중 집계한다")
	void calculateOverallCorrectRate_weightedAggregation() {
		// given: q101 첫제출 2건(정답1/오답1), q102 첫제출 2건(정답2) → 정답 3 / 전체 4 = 75.0
		QuestionEntity question1 = mockQuestion(101L);
		QuestionEntity question2 = mockQuestion(102L);
		when(answerSubmitRecordReader.getFirstSubmitsByQuestionId(any()))
			.thenReturn(Map.of(
				101L, List.of(record(101L, true), record(101L, false)),
				102L, List.of(record(102L, true), record(102L, true))));

		// when
		double rate = questionSetStatisticCalculator.calculateOverallCorrectRate(List.of(question1, question2));

		// then
		assertThat(rate).isEqualTo(75.0);
	}

	@Test
	@DisplayName("전체 정답률 - 문제가 없으면 0.0을 반환한다")
	void calculateOverallCorrectRate_emptyQuestions() {
		// when
		double rate = questionSetStatisticCalculator.calculateOverallCorrectRate(List.of());

		// then
		assertThat(rate).isEqualTo(0.0);
	}

	@Test
	@DisplayName("내 정답률 - 분모는 문제셋 전체 문제 수이며 안 푼 문제는 오답 처리한다")
	void calculateUserCorrectRate_denominatorIsAllQuestions() {
		// given: 전체 3문제 중 첫 제출 q101(정답)/q102(오답), q103 미응답 → 정답 1 / 전체 3 = 33.3
		QuestionEntity question1 = mockQuestion(101L);
		QuestionEntity question2 = mockQuestion(102L);
		QuestionEntity question3 = mockQuestion(103L);
		when(answerSubmitRecordReader.getEarliestByQuestionId(eq(USER_ID), anyList()))
			.thenReturn(Map.of(
				101L, record(101L, true),
				102L, record(102L, false)));

		// when
		Double rate = questionSetStatisticCalculator.calculateUserCorrectRate(USER_ID,
			List.of(question1, question2, question3));

		// then
		assertThat(rate).isEqualTo(33.3);
	}

	@Test
	@DisplayName("내 정답률 - 첫 제출이 한 건도 없으면(미응시) null을 반환한다")
	void calculateUserCorrectRate_returnsNullWhenNotAttempted() {
		// given
		QuestionEntity question1 = mockQuestion(101L);
		when(answerSubmitRecordReader.getEarliestByQuestionId(eq(USER_ID), anyList())).thenReturn(Map.of());

		// when
		Double rate = questionSetStatisticCalculator.calculateUserCorrectRate(USER_ID, List.of(question1));

		// then
		assertThat(rate).isNull();
	}

	@Test
	@DisplayName("내 정답률 - 문제가 없으면 null을 반환한다")
	void calculateUserCorrectRate_emptyQuestions() {
		// when
		Double rate = questionSetStatisticCalculator.calculateUserCorrectRate(USER_ID, List.of());

		// then
		assertThat(rate).isNull();
	}

	@Test
	@DisplayName("전체 정답률 배치 - 단일 조회로 문제셋별 정답률을 가중 집계한다")
	void calculateOverallCorrectRates_batch() {
		// given: set1[q101 정답, q102 오답] → 50.0, set2[q201 정답2] → 100.0
		QuestionEntity question101 = mockQuestion(101L);
		QuestionEntity question102 = mockQuestion(102L);
		QuestionEntity question201 = mockQuestion(201L);
		Map<Long, List<QuestionEntity>> questionsByQuestionSetId = Map.of(
			1L, List.of(question101, question102),
			2L, List.of(question201));
		when(answerSubmitRecordReader.getFirstSubmitsByQuestionId(any()))
			.thenReturn(Map.of(
				101L, List.of(record(101L, true)),
				102L, List.of(record(102L, false)),
				201L, List.of(record(201L, true), record(201L, true))));

		// when
		Map<Long, Double> rates = questionSetStatisticCalculator
			.calculateOverallCorrectRates(questionsByQuestionSetId);

		// then
		assertThat(rates).containsEntry(1L, 50.0).containsEntry(2L, 100.0);
		verify(answerSubmitRecordReader, times(1)).getFirstSubmitsByQuestionId(any());
	}

	@Test
	@DisplayName("내 정답률 배치 - 단일 조회로 문제셋별 정답률을 구하고 미응시 문제셋은 null이다")
	void calculateUserCorrectRates_batch() {
		// given: set1[q101 정답, q102 오답]→50.0, set2[q201 오답]→0.0, set3[q301 미응답]→null
		QuestionEntity question101 = mockQuestion(101L);
		QuestionEntity question102 = mockQuestion(102L);
		QuestionEntity question201 = mockQuestion(201L);
		QuestionEntity question301 = mockQuestion(301L);
		Map<Long, List<QuestionEntity>> questionsByQuestionSetId = Map.of(
			1L, List.of(question101, question102),
			2L, List.of(question201),
			3L, List.of(question301));
		when(answerSubmitRecordReader.getEarliestByQuestionId(eq(USER_ID), anyList()))
			.thenReturn(Map.of(
				101L, record(101L, true),
				201L, record(201L, false)));

		// when
		Map<Long, Double> rates = questionSetStatisticCalculator
			.calculateUserCorrectRates(USER_ID, questionsByQuestionSetId);

		// then
		assertThat(rates).containsEntry(1L, 50.0).containsEntry(2L, 0.0);
		assertThat(rates.get(3L)).isNull();
		verify(answerSubmitRecordReader, times(1)).getEarliestByQuestionId(eq(USER_ID), anyList());
	}

	private QuestionEntity mockQuestion(final Long id) {
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(id);
		return question;
	}

	private AnswerSubmitRecordEntity record(final Long questionId, final boolean isCorrect) {
		return AnswerSubmitRecordEntity.builder()
			.userId(USER_ID)
			.questionId(questionId)
			.isCorrect(isCorrect)
			.build();
	}
}
