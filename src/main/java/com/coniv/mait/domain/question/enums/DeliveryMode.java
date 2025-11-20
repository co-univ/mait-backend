package com.coniv.mait.domain.question.enums;

import java.util.Comparator;

import com.coniv.mait.domain.question.entity.QuestionEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DeliveryMode {
	MANAGING("문제 관리"),
	MAKING("문제 제작") {
		@Override
		public Comparator<QuestionEntity> questionComparator() {
			return LEXO_RANK_COMPARATOR;
		}
	},
	LIVE_TIME("실시간"),
	REVIEW("복습");

	private static final Comparator<QuestionEntity> LEXO_RANK_COMPARATOR = Comparator
		.comparing(QuestionEntity::getLexoRank, Comparator.nullsLast(String::compareTo));
	private static final Comparator<QuestionEntity> NUMBER_COMPARATOR = Comparator
		.comparing(QuestionEntity::getNumber, Comparator.nullsLast(Long::compareTo));

	private final String description;

	public Comparator<QuestionEntity> questionComparator() {
		return NUMBER_COMPARATOR;
	}

	public boolean isAnswerVisible() {
		return this != DeliveryMode.LIVE_TIME;
	}
}
