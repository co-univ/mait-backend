package com.coniv.mait.domain.question.enums;

import java.util.Comparator;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DeliveryMode {
	MANAGING("문제 관리", TeamUserRole.MAKER, true),
	MAKING("문제 제작", TeamUserRole.MAKER, true) {
		@Override
		public Comparator<QuestionEntity> questionComparator() {
			return LEXO_RANK_COMPARATOR;
		}
	},
	LIVE_TIME("실시간", TeamUserRole.PLAYER, false),
	STUDY("학습", TeamUserRole.PLAYER, true),
	REVIEW("복습", TeamUserRole.PLAYER, true);

	private static final Comparator<QuestionEntity> LEXO_RANK_COMPARATOR = Comparator
		.comparing(QuestionEntity::getLexoRank, Comparator.nullsLast(String::compareTo));
	private static final Comparator<QuestionEntity> NUMBER_COMPARATOR = Comparator
		.comparing(QuestionEntity::getNumber, Comparator.nullsLast(Long::compareTo));

	private final String description;

	private final TeamUserRole requiredRole;

	private final boolean answerVisible;

	public Comparator<QuestionEntity> questionComparator() {
		return NUMBER_COMPARATOR;
	}

	public boolean isAnswerVisible() {
		return answerVisible;
	}

	public static DeliveryMode from(QuestionSetSolveMode solveMode) {
		return switch (solveMode) {
			case LIVE_TIME -> DeliveryMode.LIVE_TIME;
			case STUDY -> DeliveryMode.STUDY;
		};
	}

	public static DeliveryMode resolve(QuestionSetStatus status, QuestionSetSolveMode solveMode) {
		if (status == QuestionSetStatus.MAKING || solveMode == null) {
			return MAKING;
		}

		if (status == QuestionSetStatus.REVIEW) {
			return REVIEW;
		}

		return from(solveMode);
	}
}
