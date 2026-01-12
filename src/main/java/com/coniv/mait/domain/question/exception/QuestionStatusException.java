package com.coniv.mait.domain.question.exception;

import lombok.Getter;

@Getter
public class QuestionStatusException extends RuntimeException {

	private final QuestionExceptionCode questionExceptionCode;

	public QuestionStatusException(QuestionExceptionCode questionExceptionCode) {
		super(questionExceptionCode.getMessage());
		this.questionExceptionCode = questionExceptionCode;
	}
}
