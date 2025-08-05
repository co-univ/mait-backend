package com.coniv.mait.domain.solve.service.dto;

import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnswerSubmitDto {

	private Long id;

	private Long userId;

	private Long questionId;

	private boolean isCorrect;

	public static AnswerSubmitDto from(final AnswerSubmitRecordEntity submitAnswer) {
		return AnswerSubmitDto.builder()
			.id(submitAnswer.getId())
			.userId(submitAnswer.getUserId())
			.questionId(submitAnswer.getQuestionId())
			.isCorrect(submitAnswer.isCorrect())
			.build();
	}
}
