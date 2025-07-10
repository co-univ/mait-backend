package com.coniv.mait.global.response;

import java.util.List;

import org.springframework.http.HttpStatusCode;

import com.coniv.mait.global.exception.ExceptionCode;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse extends BaseResponse {

	private final String code;
	private final String message;
	private final List<String> reasons;

	private ErrorResponse(HttpStatusCode status, String code, String message, List<String> reasons) {
		super(false, status);
		this.code = code;
		this.message = message;
		this.reasons = reasons;
	}

	public static ErrorResponse from(ExceptionCode exceptionCode) {
		return new ErrorResponse(
			exceptionCode.getStatus(),
			exceptionCode.getCode(),
			exceptionCode.getMessage(),
			null
		);
	}
}
