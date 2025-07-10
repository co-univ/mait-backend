package com.coniv.mait.global.response;

import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public abstract class BaseResponse {

	private final Boolean isSuccess;
	private final int status;

	protected BaseResponse(boolean isSuccess, HttpStatusCode status) {
		this.isSuccess = isSuccess;
		this.status = status.value();
	}
}
