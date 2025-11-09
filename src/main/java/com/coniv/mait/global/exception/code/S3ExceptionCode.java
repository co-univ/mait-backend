package com.coniv.mait.global.exception.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum S3ExceptionCode {
	PUT("사진 업로드에 실패했습니다."),
	INVALID_TYPE("지원하지 않는 확장자입니다.");

	private final String message;
}
