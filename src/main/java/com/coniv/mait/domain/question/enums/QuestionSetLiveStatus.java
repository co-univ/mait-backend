package com.coniv.mait.domain.question.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum QuestionSetLiveStatus {
	BEFORE_LIVE("실시간 퀴즈 시작 전"),
	LIVE("실시간 퀴즈 진행 중"),
	AFTER_LIVE("실시간 퀴즈 종료 후");

	private final String description;
}
