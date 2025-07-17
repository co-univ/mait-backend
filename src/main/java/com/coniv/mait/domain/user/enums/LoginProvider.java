package com.coniv.mait.domain.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LoginProvider {

	GOOGLE("구글"),
	;

	private final String description;
}
