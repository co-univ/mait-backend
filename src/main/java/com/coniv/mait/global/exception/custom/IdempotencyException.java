package com.coniv.mait.global.exception.custom;

import com.coniv.mait.global.exception.CommonExceptionCode;

import lombok.Getter;

@Getter
public class IdempotencyException extends RuntimeException {

	private final CommonExceptionCode exceptionCode;

	public IdempotencyException(CommonExceptionCode exceptionCode) {
		super(exceptionCode.getMessage());
		this.exceptionCode = exceptionCode;
	}
}
