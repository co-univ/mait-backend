package com.coniv.mait.global.exception.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum S3ImageRequestCode {
	PUT("사진 업로드에 실패했습니다.");

	private final String message;
}
