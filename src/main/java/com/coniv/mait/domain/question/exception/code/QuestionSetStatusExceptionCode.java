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
	ONLY_ONGOING(HttpStatus.BAD_REQUEST, "0003", "진행중인 문제 셋에 대해서만 처리가 가능합니다."),
	ONLY_BEFORE(HttpStatus.BAD_REQUEST, "0004", "시작 전 상태의 문제 셋만 처리가 가능합니다."),
	NEED_OPEN(HttpStatus.FORBIDDEN, "1001", "공개된 문제 셋만 풀이 가능"),
	ONLY_LIVE_TIME(HttpStatus.BAD_REQUEST, "2001", "실시간 상태의 문제 셋만 처리가 가능합니다."),
	ONLY_STUDY(HttpStatus.BAD_REQUEST, "2002", "학습 모드의 문제 셋만 처리가 가능합니다."),
	CANNOT_DELETE_ONGOING(HttpStatus.CONFLICT, "3001", "진행중인 문제 셋은 삭제할 수 없습니다. 먼저 종료해주세요.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
