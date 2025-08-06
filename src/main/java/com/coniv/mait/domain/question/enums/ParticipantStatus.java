package com.coniv.mait.domain.question.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ParticipantStatus {
	ACTIVE("참가중"),
	ELIMINATED("탈락");

	private final String description;
}
