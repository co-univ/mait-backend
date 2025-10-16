package com.coniv.mait.global.enums;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InviteTokenDuration {

	ONE_DAY("1일", 1),
	THREE_DAYS("3일", 3),
	SEVEN_DAYS("7일", 7),
	NO_EXPIRATION("만료 없음", null);

	private final String description;
	private final Integer days;

	public LocalDateTime calculateExpirationTime() {
		if (days == null) {
			return null; // 만료 없음
		}
		return LocalDateTime.now().plusDays(days);
	}
}
