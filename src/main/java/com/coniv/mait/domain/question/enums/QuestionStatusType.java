package com.coniv.mait.domain.question.enums;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QuestionStatusType {
	NOT_OPEN("시작하지 않음"),
	ACCESS_PERMISSION("접근 허용"),
	SOLVE_PERMISSION("문제 풀이 허용");

	private static final List<QuestionStatusType> OPEN_STATUSES = List.of(ACCESS_PERMISSION, SOLVE_PERMISSION);

	private final String description;

	public static List<QuestionStatusType> openStatuses() {
		return OPEN_STATUSES;
	}
}
