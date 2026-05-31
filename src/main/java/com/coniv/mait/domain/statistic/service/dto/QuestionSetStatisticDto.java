package com.coniv.mait.domain.statistic.service.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestionSetStatisticDto {

	private Long questionSetId;
	private String title;
	private QuestionSetSolveMode solveMode;
	private LocalDateTime solvedAt;
	@Builder.Default
	private List<QuestionSetWinnerDto> winners = List.of();
	private Double myCorrectRate;
	private double averageCorrectRate;

	public static QuestionSetStatisticDto of(final QuestionSetEntity questionSet,
		final List<QuestionSetParticipantEntity> winners, final Double myCorrectRate, final double averageCorrectRate) {
		return QuestionSetStatisticDto.builder()
			.questionSetId(questionSet.getId())
			.title(questionSet.getTitle())
			.solveMode(questionSet.getSolveMode())
			.solvedAt(questionSet.getEndTime())
			.winners(winners.stream()
				.map(QuestionSetWinnerDto::from)
				.toList())
			.myCorrectRate(myCorrectRate)
			.averageCorrectRate(averageCorrectRate)
			.build();
	}
}
