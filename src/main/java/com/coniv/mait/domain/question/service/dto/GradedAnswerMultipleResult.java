package com.coniv.mait.domain.question.service.dto;

public record GradedAnswerMultipleResult(
	Long number,
	boolean isCorrect
) implements GradedAnswerResult {
}

