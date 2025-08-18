package com.coniv.mait.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> extends BaseResponse {

	private final T data;

	private ApiResponse(T data) {
		super(true);
		this.data = data;
	}

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(data);
	}

	/**
	 * API 응답이 성공적이지만 데이터가 없을 때 사용
	 * 상태 코드는 200 유지
	 */
	public static <T> ApiResponse<T> noContent() {
		return new ApiResponse<>(null);
	}
}
