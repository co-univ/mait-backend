package com.coniv.mait.domain.question.exception.code;

import org.springframework.http.HttpStatus;

import com.coniv.mait.global.exception.code.ExceptionCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuestionSetCategoryExceptionCode implements ExceptionCode {

	DUPLICATE_NAME(HttpStatus.CONFLICT, "0001", "이미 존재하는 카테고리입니다."),
	DUPLICATE_NAME_DELETED(HttpStatus.CONFLICT, "0002", "삭제된 동일 이름의 카테고리가 존재합니다. 복구하시겠습니까?"),
	CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "0003", "해당 카테고리를 찾을 수 없습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
