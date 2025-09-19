package com.coniv.mait.domain.question.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DeliveryMode {
	MAKING("문제 제작"),
	LIVE_TIME("실시간"),
	REVIEW("복습");

	private final String description;

	public boolean isAnswerVisible() {
		return this != DeliveryMode.LIVE_TIME;
	}
}
