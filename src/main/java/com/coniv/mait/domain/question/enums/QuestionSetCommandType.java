package com.coniv.mait.domain.question.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QuestionSetCommandType {
	LIVE_START("실시간 문제셋 시작"),
	LIVE_END("실시간 문제셋 종료"),
	ACTIVE_PARTICIPANTS("다음 진출자 선정"),
	WINNER("우승자 선정");

	private final String description;
}
