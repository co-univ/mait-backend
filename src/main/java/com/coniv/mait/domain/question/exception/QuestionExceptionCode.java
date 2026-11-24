package com.coniv.mait.domain.question.exception;

import org.springframework.http.HttpStatus;

import com.coniv.mait.global.exception.code.ExceptionCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuestionExceptionCode implements ExceptionCode {

	UNAVAILABLE_TYPE(HttpStatus.BAD_REQUEST, "T-001", "해당 타입의 문제는 처리가 불가능합니다."),
	TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "T-002", "실제 문제 유형과 요청 유형이 일치하지 않습니다."),
	ORDER_CHANGED(HttpStatus.CONFLICT, "T-003", "문제 순서가 변경되었습니다. 목록을 새로 조회한 뒤 다시 시도해주세요."),
	ORDER_REBALANCE_REQUIRED(HttpStatus.CONFLICT, "T-004", "문제 순서 키의 재배치가 필요합니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
