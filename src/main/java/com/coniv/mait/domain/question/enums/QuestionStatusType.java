package com.coniv.mait.domain.question.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QuestionStatusType {
	WAITING("대기"),
	ACCESS_PERMISSION("접근 허용"),
	SOLVE_PERMISSION("문제 풀이 허용");

	private final String description;
}
