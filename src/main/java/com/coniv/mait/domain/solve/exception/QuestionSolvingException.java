package com.coniv.mait.domain.solve.exception;

import lombok.Getter;

@Getter
public class QuestionSolvingException extends RuntimeException {

	private final String message;

	public QuestionSolvingException(String message) {
		super(message);
		this.message = message;
	}
}
