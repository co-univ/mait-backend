package com.coniv.mait.domain.question.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStudyStatus {
	BEFORE("풀이 전"),
	ONGOING("풀이 중"),
	AFTER("풀이 완료");

	private final String description;
}
