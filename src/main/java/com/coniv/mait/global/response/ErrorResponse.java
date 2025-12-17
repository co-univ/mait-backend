package com.coniv.mait.global.response;

import java.util.List;

import org.springframework.http.HttpStatusCode;

import com.coniv.mait.global.exception.CommonExceptionCode;
import com.coniv.mait.global.exception.code.ExceptionCode;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse extends BaseResponse {

	private final HttpStatusCode status;
	private final String code;
	private final String message;
	private final List<String> reasons;

	private ErrorResponse(HttpStatusCode status, String code, String message, List<String> reasons) {
		super(false);
		this.status = status;
		this.code = code;
		this.message = message;
		this.reasons = reasons;
	}

	public static ErrorResponse from(CommonExceptionCode commonExceptionCode) {
		return new ErrorResponse(
			commonExceptionCode.getStatus(),
			commonExceptionCode.getCode(),
			commonExceptionCode.getMessage(),
			null
		);
	}

	public static ErrorResponse of(CommonExceptionCode commonExceptionCode, List<String> reasons) {
		return new ErrorResponse(
			commonExceptionCode.getStatus(),
			commonExceptionCode.getCode(),
			commonExceptionCode.getMessage(),
			reasons
		);
	}

	public static ErrorResponse from(ExceptionCode exceptionCode) {
		return ErrorResponse.builder()
			.status(exceptionCode.getStatus())
			.message(exceptionCode.getMessage())
			.code(exceptionCode.getCode())
			.build();
	}
}
