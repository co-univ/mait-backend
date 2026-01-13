package com.coniv.mait.domain.question.service.dto;

public record GradedAnswerFillBlankResult(
	Long number,
	String answer,
	boolean isCorrect
) implements GradedAnswerResult {
}

