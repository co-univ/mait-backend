package com.coniv.mait.global.response;

import lombok.Getter;

@Getter
public abstract class BaseResponse {

	private final Boolean isSuccess;

	protected BaseResponse(boolean isSuccess) {
		this.isSuccess = isSuccess;
	}
}
