package com.coniv.mait.global.exception.custom;

import lombok.Getter;

@Getter
public class DistributedLockException extends RuntimeException {

	private final String lockKey;

	public DistributedLockException(final String lockKey) {
		super("분산락 획득에 실패했습니다. key=" + lockKey);
		this.lockKey = lockKey;
	}
}
