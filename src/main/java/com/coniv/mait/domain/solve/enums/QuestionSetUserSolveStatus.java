package com.coniv.mait.domain.solve.enums;

public enum QuestionSetUserSolveStatus {
	NOT_STARTED,
	IN_PROGRESS,
	COMPLETED;

	public static QuestionSetUserSolveStatus from(SolvingStatus solvingStatus) {
		return switch (solvingStatus) {
			case PROGRESSING -> IN_PROGRESS;
			case COMPLETE -> COMPLETED;
		};
	}
}
