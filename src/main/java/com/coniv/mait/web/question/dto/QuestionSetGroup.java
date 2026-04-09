package com.coniv.mait.web.question.dto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "문제 셋 그룹 (진행 상태별로 그룹화된 Map 구조)")
public record QuestionSetGroup(
		@Schema(description = "상태별로 그룹화된 문제 셋")
		Map<QuestionSetStatus, List<QuestionSetDto>> questionSets) implements QuestionSetContainer {

	public static QuestionSetGroup of(List<QuestionSetDto> questionSets) {
		Map<QuestionSetStatus, List<QuestionSetDto>> questionSetsByStatus = questionSets.stream()
				.collect(Collectors.groupingBy(QuestionSetDto::getStatus));
		return QuestionSetGroup.builder()
				.questionSets(questionSetsByStatus)
				.build();
	}
}
