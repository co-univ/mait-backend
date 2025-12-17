package com.coniv.mait.domain.solve.exception;

import com.coniv.mait.global.exception.code.ExceptionCode;

import lombok.Getter;

@Getter
public class QuestionSolvingException extends RuntimeException {

	private final ExceptionCode exceptionCode;

	public QuestionSolvingException(ExceptionCode exceptionCode) {
		super(exceptionCode.getMessage());
		this.exceptionCode = exceptionCode;
	}
}
