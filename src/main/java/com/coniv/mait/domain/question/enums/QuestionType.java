package com.coniv.mait.domain.question.enums;

import com.coniv.mait.domain.question.constant.QuestionConstant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QuestionType {
	SHORT(QuestionConstant.SHORT),
	MULTIPLE(QuestionConstant.MULTIPLE),
	ORDERING(QuestionConstant.ORDERING),
	FILL_BLANK(QuestionConstant.FILL_BLANK);

	private final String type;

	@Override
	public String toString() {
		return name();
	}
}
