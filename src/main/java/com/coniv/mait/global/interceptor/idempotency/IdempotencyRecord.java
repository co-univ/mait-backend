package com.coniv.mait.global.interceptor.idempotency;

import java.util.UUID;

public record IdempotencyRecord(
	IdempotencyStatus status,
	String owner,
	String fingerprint,
	IdempotencyResponse response
) {
	public static IdempotencyRecord processing(String fingerprint) {
		return new IdempotencyRecord(IdempotencyStatus.PROCESSING, UUID.randomUUID().toString(),
			fingerprint, null);
	}

	public IdempotencyRecord completed(IdempotencyResponse response) {
		return new IdempotencyRecord(IdempotencyStatus.COMPLETED, owner, fingerprint, response);
	}
}
