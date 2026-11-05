package com.coniv.mait.domain.question.exception;

import lombok.Getter;

@Getter
public class QuestionException extends RuntimeException {

	private final QuestionExceptionCode exceptionCode;

	public QuestionException(QuestionExceptionCode exceptionCode) {
		super(exceptionCode.getMessage());
		this.exceptionCode = exceptionCode;
	}
}
