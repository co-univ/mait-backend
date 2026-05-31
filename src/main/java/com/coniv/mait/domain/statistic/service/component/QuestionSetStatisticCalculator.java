package com.coniv.mait.domain.statistic.service.component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.service.component.AnswerSubmitRecordReader;
import com.coniv.mait.global.util.MathUtil;

import lombok.RequiredArgsConstructor;

/**
 * 문제셋의 정답률(%)을 계산한다.
 *
 * <p>정답 판정은 통계 기준인 <b>첫 제출(submitOrder 최소)</b>의 정오답을 따른다.
 * (문제별 오답률 로직과 동일 기준이며, {@code 전체 정답률 = 100 - 전체 오답률} 관계가 성립한다.)
 *
 * <p><b>전체 정답률</b>은 문제셋의 모든 문제에 대한 (유저별) 첫 제출을 통째로 모아
 * {@code 정답 첫 제출 수 / 전체 첫 제출 수} 로 구하는 가중 방식이다.
 * 문제별 정답률을 단순 평균하면 응시자 수가 적은 문제에 과대 가중이 걸리므로 통째 집계를 사용한다.
 *
 * <p><b>개인 정답률</b>은 분모가 문제셋의 전체 문제 수이며(안 푼 문제는 오답 처리), 미응시면 null이다.
 *
 * <p>여러 문제셋을 한 번에 처리하는 배치 메서드는 전체 문제의 제출 기록을 한 번의 조회로 모은 뒤
 * 문제셋별로 분할 집계하여 N+1을 피한다.
 */
@Component
@RequiredArgsConstructor
public class QuestionSetStatisticCalculator {

	private final AnswerSubmitRecordReader answerSubmitRecordReader;

	/**
	 * 문제셋의 전체 정답률(%)을 첫 제출 기준으로 계산한다. 제출 기록이 없으면 0.0을 반환한다.
	 */
	public double calculateOverallCorrectRate(final List<QuestionEntity> questions) {
		return overallRate(questions, answerSubmitRecordReader.getFirstSubmitsByQuestionId(questions));
	}

	/**
	 * 특정 유저가 문제셋에서 푼 정답률(%)을 첫 제출 기준으로 계산한다. 미응시면 0%와 구분하기 위해 null을 반환한다.
	 */
	public Double calculateUserCorrectRate(final Long userId, final List<QuestionEntity> questions) {
		if (questions.isEmpty()) {
			return null;
		}
		List<Long> questionIds = questions.stream().map(QuestionEntity::getId).toList();
		return userRate(questions, answerSubmitRecordReader.getEarliestByQuestionId(userId, questionIds));
	}

	/**
	 * 여러 문제셋의 전체 정답률을 문제셋 ID별로 한 번에 계산한다(제출 기록 단일 조회).
	 */
	public Map<Long, Double> calculateOverallCorrectRates(
		final Map<Long, List<QuestionEntity>> questionsByQuestionSetId) {
		Map<Long, List<AnswerSubmitRecordEntity>> firstSubmitsByQuestionId =
			fetchFirstSubmits(questionsByQuestionSetId);

		Map<Long, Double> ratesByQuestionSetId = new HashMap<>();
		questionsByQuestionSetId.forEach((questionSetId, questions) ->
			ratesByQuestionSetId.put(questionSetId, overallRate(questions, firstSubmitsByQuestionId)));
		return ratesByQuestionSetId;
	}

	/**
	 * 특정 유저의 여러 문제셋 정답률을 문제셋 ID별로 한 번에 계산한다(제출 기록 단일 조회). 미응시 문제셋은 값이 null이다.
	 */
	public Map<Long, Double> calculateUserCorrectRates(final Long userId,
		final Map<Long, List<QuestionEntity>> questionsByQuestionSetId) {
		List<Long> questionIds = questionsByQuestionSetId.values().stream()
			.flatMap(List::stream)
			.map(QuestionEntity::getId)
			.toList();
		Map<Long, AnswerSubmitRecordEntity> earliestByQuestionId = questionIds.isEmpty() ? Map.of()
			: answerSubmitRecordReader.getEarliestByQuestionId(userId, questionIds);

		Map<Long, Double> ratesByQuestionSetId = new HashMap<>();
		questionsByQuestionSetId.forEach((questionSetId, questions) ->
			ratesByQuestionSetId.put(questionSetId, userRate(questions, earliestByQuestionId)));
		return ratesByQuestionSetId;
	}

	private Map<Long, List<AnswerSubmitRecordEntity>> fetchFirstSubmits(
		final Map<Long, List<QuestionEntity>> questionsByQuestionSetId) {
		List<QuestionEntity> questions = questionsByQuestionSetId.values().stream()
			.flatMap(List::stream)
			.toList();
		if (questions.isEmpty()) {
			return Map.of();
		}
		return answerSubmitRecordReader.getFirstSubmitsByQuestionId(questions);
	}

	private double overallRate(final List<QuestionEntity> questions,
		final Map<Long, List<AnswerSubmitRecordEntity>> firstSubmitsByQuestionId) {
		List<AnswerSubmitRecordEntity> firstSubmits = questions.stream()
			.flatMap(question -> firstSubmitsByQuestionId.getOrDefault(question.getId(), List.of()).stream())
			.toList();

		long correctCount = firstSubmits.stream()
			.filter(AnswerSubmitRecordEntity::isCorrect)
			.count();

		return MathUtil.calculateRateRoundedToFirstDecimal(correctCount, firstSubmits.size());
	}

	private Double userRate(final List<QuestionEntity> questions,
		final Map<Long, AnswerSubmitRecordEntity> earliestByQuestionId) {
		List<AnswerSubmitRecordEntity> mySubmits = questions.stream()
			.map(question -> earliestByQuestionId.get(question.getId()))
			.filter(Objects::nonNull)
			.toList();
		if (mySubmits.isEmpty()) {
			return null;
		}

		long correctCount = mySubmits.stream()
			.filter(AnswerSubmitRecordEntity::isCorrect)
			.count();

		return MathUtil.calculateRateRoundedToFirstDecimal(correctCount, questions.size());
	}
}
