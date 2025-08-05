package com.coniv.mait.domain.question.dto;

import com.coniv.mait.domain.question.enums.QuestionStatusType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionStatusMessage {
	private Long questionSetId;
	private Long questionId;
	private QuestionStatusType statusType;
}
