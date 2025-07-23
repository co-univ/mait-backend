package com.coniv.mait.domain.team.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TeamUserRole {

	MAKER("문제 관리자"),

	OWNER("팀 생성자"),

	PLAYER("참가자");

	private final String description;
}
