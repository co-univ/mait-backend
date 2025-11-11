package com.coniv.mait.domain.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum PolicyTiming {

	SIGN_UP("회원가입 시 동의하는 정책");

	private final String description;
}
