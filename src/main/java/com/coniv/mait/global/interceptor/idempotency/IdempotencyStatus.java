package com.coniv.mait.global.interceptor.idempotency;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IdempotencyStatus {
	PROCESSING("요청 처리 중"),
	COMPLETED("요청 완료");

	private final String description;
}
