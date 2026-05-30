package com.coniv.mait.domain.statistic.service.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.solve.service.dto.QuestionSolveResultDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MySolveRecordDto {

	private Long questionSetId;
	private QuestionSetSolveMode solveMode;
	private int totalCount;
	private int correctCount;
	private double score;
	private List<QuestionSolveResultDto> results;

	public static MySolveRecordDto of(final Long questionSetId, final QuestionSetSolveMode solveMode,
		final int totalCount, final int correctCount, final List<QuestionSolveResultDto> results) {
		return MySolveRecordDto.builder()
			.questionSetId(questionSetId)
			.solveMode(solveMode)
			.totalCount(totalCount)
			.correctCount(correctCount)
			.score(calculateScore(totalCount, correctCount))
			.results(results)
			.build();
	}

	/**
	 * 100점 만점 기준 점수를 소수 둘째 자리에서 반올림하여 소수 첫째 자리까지 계산한다. (예: 3/7 → 42.9)
	 */
	private static double calculateScore(final int totalCount, final int correctCount) {
		if (totalCount <= 0) {
			return 0.0;
		}

		return BigDecimal.valueOf((double)correctCount * 100 / totalCount)
			.setScale(1, RoundingMode.HALF_UP)
			.doubleValue();
	}
}
