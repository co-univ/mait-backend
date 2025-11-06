package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.service.dto.QuestionSetDto;

import lombok.Builder;

@Builder
public record QuestionSetList(
	List<QuestionSetDto> questionSets
) implements QuestionSetContainer {

	public static QuestionSetList of(List<QuestionSetDto> questionSets) {
		return QuestionSetList.builder()
			.questionSets(questionSets)
			.build();
	}
}
