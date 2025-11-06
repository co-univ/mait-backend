package com.coniv.mait.web.question.dto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.coniv.mait.domain.question.enums.QuestionSetOngoingStatus;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;

import lombok.Builder;

@Builder
public record QuestionSetGroup(
		Map<QuestionSetOngoingStatus, List<QuestionSetDto>> questionSets) implements QuestionSetContainer {

	public static QuestionSetGroup of(List<QuestionSetDto> questionSets) {
		Map<QuestionSetOngoingStatus, List<QuestionSetDto>> questionSetsByStatus = questionSets.stream()
				.collect(Collectors.groupingBy(QuestionSetDto::getOngoingStatus));
		return QuestionSetGroup.builder()
				.questionSets(questionSetsByStatus)
				.build();
	}
}
