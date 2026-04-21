package com.coniv.mait.domain.question.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QuestionSetStatus {
	BEFORE("풀이 전"),
	ONGOING("풀이 중"),
	AFTER("풀이 완료"),
	REVIEW("복습 단계");

	private final String description;

	public boolean isCompleted() {
		return this == AFTER || this == REVIEW;
	}
}
