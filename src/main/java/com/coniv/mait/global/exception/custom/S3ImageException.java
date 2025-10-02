package com.coniv.mait.global.exception.custom;

import com.coniv.mait.global.exception.code.S3ImageRequestCode;

import lombok.Getter;

@Getter
public class S3ImageException extends RuntimeException {

	private final S3ImageRequestCode code;

	private final String bucket;

	private final String key;

	public S3ImageException(S3ImageRequestCode code, String bucket, String key) {
		super(code.getMessage());
		this.code = code;
		this.bucket = bucket;
		this.key = key;
	}
}
