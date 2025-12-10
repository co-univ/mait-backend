package com.coniv.mait.domain.solve.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuestionSolveExceptionCode {
	NOT_PARTICIPATE("문제 풀이가 불가능한 유저");

	private final String description;
}
