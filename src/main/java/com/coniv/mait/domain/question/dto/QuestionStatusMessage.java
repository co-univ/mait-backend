package com.coniv.mait.domain.question.dto;

import com.coniv.mait.domain.question.enums.QuestionStatusType;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QuestionStatusMessage {
	private Long questionSetId;
	private Long questionId;
	private QuestionStatusType statusType;

	@Builder
	public QuestionStatusMessage(Long questionSetId, Long questionId, QuestionStatusType statusType) {
		this.questionSetId = questionSetId;
		this.questionId = questionId;
		this.statusType = statusType;
	}
}
