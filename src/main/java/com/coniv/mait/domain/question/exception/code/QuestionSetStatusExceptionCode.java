package com.coniv.mait.domain.question.exception.code;

import org.springframework.http.HttpStatus;

import com.coniv.mait.global.exception.code.ExceptionCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuestionSetStatusExceptionCode implements ExceptionCode {

	ONLY_AFTER(HttpStatus.BAD_REQUEST, "0001", "종료된 문제 셋만 접근 가능합니다."),
	ONLY_REVIEW(HttpStatus.BAD_REQUEST, "0002", "리뷰 상태의 문제 셋만 처리가 가능합니다."),
	NEED_OPEN(HttpStatus.FORBIDDEN, "1001", "");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
