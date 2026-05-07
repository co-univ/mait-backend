package com.coniv.mait.domain.question.exception;

import com.coniv.mait.domain.question.exception.code.QuestionSetCategoryExceptionCode;

import lombok.Getter;

@Getter
public class QuestionSetCategoryException extends RuntimeException {

	private final QuestionSetCategoryExceptionCode exceptionCode;

	public QuestionSetCategoryException(QuestionSetCategoryExceptionCode exceptionCode) {
		super(exceptionCode.getMessage());
		this.exceptionCode = exceptionCode;
	}
}
