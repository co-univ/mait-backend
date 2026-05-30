package com.coniv.mait.domain.statistic.service.dto;

import java.util.List;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionStatisticDto {

	private Long questionId;

	private Long questionNumber;

	private long submittedUserCount;

	private long firstWrongUserCount;

	private Double wrongRate;

	public static QuestionStatisticDto of(final QuestionEntity question,
		final List<AnswerSubmitRecordEntity> firstSubmits) {
		List<AnswerSubmitRecordEntity> submits = firstSubmits == null ? List.of() : firstSubmits;
		long submittedUserCount = submits.size();
		long firstWrongUserCount = submits.stream()
			.filter(record -> !record.isCorrect())
			.count();

		return QuestionStatisticDto.builder()
			.questionId(question.getId())
			.questionNumber(question.getNumber())
			.submittedUserCount(submittedUserCount)
			.firstWrongUserCount(firstWrongUserCount)
			.wrongRate(calculateWrongRate(submittedUserCount, firstWrongUserCount))
			.build();
	}

	private static Double calculateWrongRate(final long submittedUserCount, final long firstWrongUserCount) {
		if (submittedUserCount == 0) {
			return null;
		}
		// 소수점 첫째 자리까지 반올림
		return Math.round((double)firstWrongUserCount / submittedUserCount * 1000) / 10.0;
	}
}
