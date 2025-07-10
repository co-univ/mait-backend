package com.coniv.mait.domain.question.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QuestionSetVisibility {

	PUBLIC("공개"),
	GROUP("그룹 공개"),
	PRIVATE("비공개");

	private final String description;
}
