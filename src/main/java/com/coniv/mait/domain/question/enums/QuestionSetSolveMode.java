package com.coniv.mait.domain.question.enums;

public enum QuestionSetSolveMode {
	LIVE_TIME,
	STUDY;

	public DeliveryMode toDeliveryMode() {
		return switch (this) {
			case LIVE_TIME -> DeliveryMode.LIVE_TIME;
			case STUDY -> DeliveryMode.STUDY;
		};
	}
}
