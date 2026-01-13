package com.coniv.mait.domain.question.service.dto;

public record GradedAnswerShortResult(
	String answer,
	boolean isCorrect
) implements GradedAnswerResult {
}

