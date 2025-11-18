package com.coniv.mait.domain.team.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InvitationApplicationStatus {

	PENDING("대기 중"),

	APPROVED("승인"),

	REJECTED("거절");

	private final String description;
}
