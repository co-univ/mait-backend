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
public class QuestionSolveResultDto {

	private Long questionId;
	private boolean isCorrect;
	private String submittedAnswer;

	public static QuestionSolveResultDto from(final AnswerSubmitRecordEntity record) {
		return QuestionSolveResultDto.builder()
			.questionId(record.getQuestionId())
			.isCorrect(record.isCorrect())
			.submittedAnswer(record.getSubmittedAnswer())
			.build();
	}

	public static QuestionSolveResultDto unanswered(final Long questionId) {
		return QuestionSolveResultDto.builder()
			.questionId(questionId)
			.isCorrect(false)
			.submittedAnswer(null)
			.build();
	}
}
