package com.coniv.mait.domain.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PolicyCategory {

	TERMS_OF_SERVICE("서비스 이용약관 관련된 정책"),
	PERSONAL_INFORMATION("개인정보 관련된 정책"),
	MARKETING("마케팅 활용 관련된 정책");

	private final String description;
}
