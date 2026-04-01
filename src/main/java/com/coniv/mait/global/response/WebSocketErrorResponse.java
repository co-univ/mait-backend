package com.coniv.mait.global.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class WebSocketErrorResponse {

	private final boolean success;
	private final String message;

	private WebSocketErrorResponse(boolean success, String message) {
		this.success = success;
		this.message = message;
	}

	public static WebSocketErrorResponse from(String message) {
		return new WebSocketErrorResponse(false, message);
	}
}
