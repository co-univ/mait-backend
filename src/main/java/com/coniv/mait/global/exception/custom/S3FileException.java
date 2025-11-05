package com.coniv.mait.global.exception.custom;

import com.coniv.mait.global.exception.code.S3ExceptionCode;

import lombok.Getter;

@Getter
public class S3FileException extends RuntimeException {

	private final S3ExceptionCode code;

	private final String bucket;

	private final String key;

	public S3FileException(S3ExceptionCode code, String bucket, String key) {
		super(code.getMessage());
		this.code = code;
		this.bucket = bucket;
		this.key = key;
	}
}
