package com.coniv.mait.web.question.dto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.coniv.mait.domain.question.enums.QuestionSetOngoingStatus;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "문제 셋 그룹 (진행 상태별로 그룹화된 Map 구조)")
public record QuestionSetGroup(
		@Schema(description = "진행 상태별로 그룹화된 문제 셋 (BEFORE: 시작 전, ONGOING: 진행 중, AFTER: 종료)")
		Map<QuestionSetOngoingStatus, List<QuestionSetDto>> questionSets) implements QuestionSetContainer {

	public static QuestionSetGroup of(List<QuestionSetDto> questionSets) {
		Map<QuestionSetOngoingStatus, List<QuestionSetDto>> questionSetsByStatus = questionSets.stream()
				.collect(Collectors.groupingBy(QuestionSetDto::getOngoingStatus));
		return QuestionSetGroup.builder()
				.questionSets(questionSetsByStatus)
				.build();
	}
}
