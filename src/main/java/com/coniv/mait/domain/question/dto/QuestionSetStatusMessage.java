package com.coniv.mait.domain.question.dto;

import com.coniv.mait.domain.question.enums.QuestionSetCommandType;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QuestionSetStatusMessage {
	private Long questionSetId;
	private QuestionSetCommandType commandType;

	@Builder
	public QuestionSetStatusMessage(Long questionSetId, QuestionSetCommandType commandType) {
		this.questionSetId = questionSetId;
		this.commandType = commandType;
	}
}
