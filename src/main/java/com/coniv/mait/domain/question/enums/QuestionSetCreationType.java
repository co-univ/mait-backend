package com.coniv.mait.domain.question.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QuestionSetCreationType {
	AI_GENERATED("AI 추천 요청"),
	MANUAL("사용자 직접 제작");

	private final String description;
}
