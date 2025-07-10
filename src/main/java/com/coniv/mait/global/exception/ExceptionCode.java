package com.coniv.mait.global.exception;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExceptionCode {
	USER_INPUT_EXCEPTION(HttpStatus.BAD_REQUEST, "C-001", "사용자 입력 오류입니다."),
	UNEXPECTED_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "S-000", "예기치 못한 오류가 발생했습니다."),
	;

	private final HttpStatus status;
	private final String code;
	private final String message;
}
