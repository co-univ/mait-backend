package com.coniv.mait.domain.solve.exception;

import org.springframework.http.HttpStatus;

import com.coniv.mait.global.exception.code.ExceptionCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuestionSolveExceptionCode implements ExceptionCode {
	CANNOT_SOLVE(HttpStatus.BAD_REQUEST, "아직 문제를 풀 수 없습니다."),
	NOT_PARTICIPATED(HttpStatus.FORBIDDEN, "풀이에 참여중이지 않은 유저입니다."),
	ALREADY(HttpStatus.CONFLICT, "이미 정답 처리 기록이 존재합니다.");

	private final HttpStatus status;
	private final String message;
}
