package com.coniv.mait.domain.team.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TeamRole {

	MAKER("퀴즈 생성 가능자"),

	OWNER("생성자"),

	PLAYER("참가자");

	private final String description;
}
