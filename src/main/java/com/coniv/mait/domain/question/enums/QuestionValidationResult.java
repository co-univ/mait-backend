package com.coniv.mait.domain.question.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QuestionValidationResult {
	EMPTY_CONTENT("질문을 입력해주세요."),

	// 객관식
	INVALID_CHOICE_COUNT("객관식 문제의 선택지는 2개 이상 8개 이하입니다."),
	EMPTY_CHOICE_CONTENT("객관식 문제의 모든 선택지 내용을 입력해주세요."),
	NO_CORRECT_CHOICE("객관식 문제의 정답이 하나 이상 선택되어야 합니다."),

	// 주관식
	INVALID_ANSWER_COUNT("주관식 문제의 정답은 1개 이상이어야 합니다."),
	EMPTY_SHORT_ANSWER_CONTENT("주관식 문제의 모든 정답 내용을 입력해주세요."),

	INVALID_SUB_ANSWER_COUNT("주관식 문제 인정 답안은 하나의 번호당 5개 이하이어야 합니다."),

	// 빈칸
	INVALID_BLANK_COUNT("빈칸 문제의 빈칸 개수는 1개 이상이어야 합니다."),
	EMPTY_FILL_BLANK_ANSWER_CONTENT("빈칸 문제의 모든 빈칸 정답 내용을 입력해주세요."),
	INVALID_FILL_BLANK_SUB_ANSWER_COUNT("빈칸 인정 답안은 하나의 번호당 5개 이하이어야 합니다."),

	// 순서
	INVALID_OPTION_COUNT("순서 문제의 항목은 2개 이상이어야 합니다."),
	EMPTY_ORDERING_OPTION_CONTENT("순서 문제의 모든 항목 내용을 입력해주세요.");

	private final String description;
}
