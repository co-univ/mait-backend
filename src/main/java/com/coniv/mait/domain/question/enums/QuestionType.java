package com.coniv.mait.domain.question.enums;

import com.coniv.mait.domain.question.constant.QuestionConstant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QuestionType {
	SHORT(QuestionContstant.SHORT),
	MULTIPLE(QuestionContstant.MULTIPLE),
	ORDERING(QuestionContstant.ORDERING),
	FILL_BLANK(QuestionContstant.FILL_BLANK);

	private final String type;
}
