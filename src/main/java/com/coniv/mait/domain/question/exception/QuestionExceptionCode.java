package com.coniv.mait.domain.question.exception;

import org.springframework.http.HttpStatus;

import com.coniv.mait.global.exception.code.ExceptionCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuestionExceptionCode implements ExceptionCode {

	UNAVAILABLE_TYPE(HttpStatus.BAD_REQUEST, "T-001", "해당 타입의 문제는 처리가 불가능합니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
