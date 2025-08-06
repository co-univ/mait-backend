package com.coniv.mait.domain.question.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QuestionSetCommandType {
	LIVE_START("실시간 문제셋 시작"),
	LIVE_END("실시간 문제셋 종료");

	private final String description;
}
