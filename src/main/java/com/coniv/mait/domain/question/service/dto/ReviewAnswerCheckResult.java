package com.coniv.mait.domain.question.service.dto;

import java.util.List;

import com.coniv.mait.domain.question.enums.QuestionType;

import lombok.Builder;

@Builder
public record ReviewAnswerCheckResult(
	Long questionId,
	boolean isCorrect,
	QuestionType type,
	List<? extends GradedAnswerResult> gradedResults
) {
}

