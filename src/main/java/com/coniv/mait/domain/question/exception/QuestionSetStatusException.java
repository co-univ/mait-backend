package com.coniv.mait.domain.question.exception;

import com.coniv.mait.domain.question.exception.code.QuestionSetStatusExceptionCode;

import lombok.Getter;

@Getter
public class QuestionSetStatusException extends RuntimeException {

	private final QuestionSetStatusExceptionCode exceptionCode;

	public QuestionSetStatusException(QuestionSetStatusExceptionCode exceptionCode) {
		super(exceptionCode.getMessage());
		this.exceptionCode = exceptionCode;
	}
}
