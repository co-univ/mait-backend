package com.coniv.mait.domain.question.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
	description = "채점된 답안 결과",
	oneOf = {
		GradedAnswerShortResult.class,
		GradedAnswerMultipleResult.class,
		GradedAnswerFillBlankResult.class
	}
)
public interface GradedAnswerResult {
	boolean isCorrect();
}
