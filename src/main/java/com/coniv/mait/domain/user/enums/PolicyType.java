package com.coniv.mait.domain.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum PolicyType {

	ESSENTIAL("필수 선택 정책"),
	OPTIONAL("선택적인 정책");

	private final String description;
}
