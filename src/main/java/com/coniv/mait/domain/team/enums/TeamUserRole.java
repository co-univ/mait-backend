package com.coniv.mait.domain.team.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TeamUserRole {

	MAKER("문제 관리자", 1),

	OWNER("팀 생성자", 2),

	PLAYER("참가자", 0);

	private final String description;
	private final int level;

	public boolean canCreateQuestionSet() {
		return this == MAKER || this == OWNER;
	}

	public boolean covers(final TeamUserRole required) {
		return this.level >= required.level;
	}
}
