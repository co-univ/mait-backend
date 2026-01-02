package com.coniv.mait.domain.solve.exception;

import org.springframework.http.HttpStatus;

import com.coniv.mait.global.exception.code.ExceptionCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuestionSolveExceptionCode implements ExceptionCode {
	INVALID_TYPE(HttpStatus.BAD_REQUEST, "T-001", "풀려는 문제와 유형이 다릅니다."),

	CANNOT_SOLVE(HttpStatus.BAD_REQUEST, "QS-001", "아직 문제를 풀 수 없습니다."),
	NOT_PARTICIPATED(HttpStatus.FORBIDDEN, "QS-002", "풀이에 참여중이지 않은 유저입니다."),
	ALREADY(HttpStatus.CONFLICT, "QS-003", "이미 정답 처리 기록이 존재합니다."),

	ANSWER_COUNT(HttpStatus.BAD_REQUEST, "S-001", "제출된 답변의 개수와 문제의 답변 개수가 일치하지 않습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
