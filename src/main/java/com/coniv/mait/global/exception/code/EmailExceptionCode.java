package com.coniv.mait.global.exception.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmailExceptionCode {
	SEND("이메일 발송에 실패했습니다.");

	private final String message;
}
