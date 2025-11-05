package com.coniv.mait.domain.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PolicyCategory {

	PERSONAL_INFORMATION("개인정보 관련된 정책");

	private final String description;
}
