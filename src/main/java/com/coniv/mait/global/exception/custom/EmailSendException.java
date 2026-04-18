package com.coniv.mait.global.exception.custom;

import com.coniv.mait.global.exception.code.EmailExceptionCode;

import lombok.Getter;

@Getter
public class EmailSendException extends RuntimeException {

	private final EmailExceptionCode code;

	private final String provider;

	public EmailSendException(EmailExceptionCode code, String provider, Throwable cause) {
		super(code.getMessage(), cause);
		this.code = code;
		this.provider = provider;
	}
}
