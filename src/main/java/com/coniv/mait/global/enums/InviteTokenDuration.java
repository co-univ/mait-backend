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

	/**
	 * 현재 시간으로부터 만료 시간을 계산합니다.
	 *
	 * @return 만료 시간 (만료 없음인 경우 null)
	 */
	public LocalDateTime calculateExpirationTime() {
		if (days == null) {
			return null; // 만료 없음
		}
		return LocalDateTime.now().plusDays(days);
	}

	/**
	 * 특정 시간으로부터 만료 시간을 계산합니다.
	 *
	 * @param baseTime 기준 시간
	 * @return 만료 시간 (만료 없음인 경우 null)
	 */
	public LocalDateTime calculateExpirationTime(LocalDateTime baseTime) {
		if (days == null) {
			return null; // 만료 없음
		}
		return baseTime.plusDays(days);
	}

	/**
	 * 만료되지 않는 초대 코드인지 확인합니다.
	 *
	 * @return 만료 없음 여부
	 */
	public boolean isNeverExpires() {
		return days == null;
	}
}
