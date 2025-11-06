package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.service.dto.QuestionSetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "문제 셋 목록 (List 구조)")
public record QuestionSetList(
	@Schema(description = "문제 셋 목록")
	List<QuestionSetDto> questionSets
) implements QuestionSetContainer {

	public static QuestionSetList of(List<QuestionSetDto> questionSets) {
		return QuestionSetList.builder()
			.questionSets(questionSets)
			.build();
	}
}
