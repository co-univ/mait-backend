package com.coniv.mait.domain.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PendingSignupKey {
	SIGNUP("회원가입"),
	LOGIN("로그인");

	private final String description;
}
