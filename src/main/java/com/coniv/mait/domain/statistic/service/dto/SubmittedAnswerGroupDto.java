package com.coniv.mait.domain.statistic.service.dto;

import java.util.List;

import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubmittedAnswerGroupDto {

	private final long submitCount;
	private final boolean isCorrect;
	private final SubmitAnswerDto<?> submittedAnswer;

	public static SubmittedAnswerGroupDto of(final String submittedAnswerJson,
		final List<AnswerSubmitRecordEntity> records) {
		return SubmittedAnswerGroupDto.builder()
			.submitCount(records.size())
			.isCorrect(records.get(0).isCorrect())
			.submittedAnswer(SubmitAnswerDto.fromJson(submittedAnswerJson))
			.build();
	}
}
