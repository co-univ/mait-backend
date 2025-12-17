package com.coniv.mait.global.exception.code;

import org.springframework.http.HttpStatus;

public interface ExceptionCode {
	HttpStatus status = null;
	String message = null;

	HttpStatus getStatus();

	String getMessage();
}
