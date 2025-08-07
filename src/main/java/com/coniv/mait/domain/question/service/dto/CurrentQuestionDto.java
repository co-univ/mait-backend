package com.coniv.mait.domain.question.service.dto;

import com.coniv.mait.domain.question.enums.QuestionStatusType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CurrentQuestionDto {
	private Long questionSetId;
	private Long questionId;
	private QuestionStatusType questionStatus;

	public static CurrentQuestionDto of(Long questionSetId, Long questionId, QuestionStatusType questionStatus) {
		return CurrentQuestionDto.builder()
			.questionSetId(questionSetId)
			.questionId(questionId)
			.questionStatus(questionStatus)
			.build();
	}

	public static CurrentQuestionDto notOpenQuestion(Long questionSetId) {
		return CurrentQuestionDto.builder()
			.questionSetId(questionSetId)
			.questionId(null)
			.questionStatus(QuestionStatusType.NOT_OPEN)
			.build();
	}
}
