package com.coniv.mait.domain.question.enums;

import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStudyStatus {
	BEFORE("풀이 전"),
	ONGOING("풀이 중"),
	AFTER("풀이 완료");

	private final String description;

	public static UserStudyStatus from(SolvingSessionEntity solvingSession) {
		if (solvingSession == null) {
			return BEFORE;
		}

		return switch (solvingSession.getStatus()) {
			case PROGRESSING -> UserStudyStatus.ONGOING;
			case COMPLETE -> UserStudyStatus.AFTER;
		};
	}
}
