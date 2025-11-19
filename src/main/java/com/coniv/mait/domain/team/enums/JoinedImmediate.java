package com.coniv.mait.domain.team.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum JoinedImmediate {
	IMMEDIATE("즉시 참가"),
	APPROVAL_REQUIRED("승인 필요");

	private final String description;
}
