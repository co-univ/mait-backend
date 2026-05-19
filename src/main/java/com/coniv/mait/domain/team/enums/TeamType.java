package com.coniv.mait.domain.team.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TeamType {

	PERSONAL("개인 워크스페이스"),

	GROUP("단체 팀");

	private final String description;
}
