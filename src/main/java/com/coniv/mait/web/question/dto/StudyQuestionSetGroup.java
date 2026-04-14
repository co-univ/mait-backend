package com.coniv.mait.web.question.dto;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.coniv.mait.domain.question.enums.UserStudyStatus;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "문제 셋 그룹 (학습 모드 - 사용자별 풀이 상태로 그룹화된 Map 구조)")
public record StudyQuestionSetGroup(
	@Schema(description = "사용자 풀이 상태별로 그룹화된 문제 셋")
	Map<UserStudyStatus, List<QuestionSetDto>> questionSets) implements QuestionSetContainer {

	public static StudyQuestionSetGroup from(final List<QuestionSetDto> questionSets) {
		Map<UserStudyStatus, List<QuestionSetDto>> grouped = questionSets.stream()
			.collect(Collectors.groupingBy(
				QuestionSetDto::getUserStudyStatus,
				() -> new EnumMap<>(UserStudyStatus.class),
				Collectors.toList()
			));
		for (UserStudyStatus status : UserStudyStatus.values()) {
			grouped.putIfAbsent(status, List.of());
		}
		return StudyQuestionSetGroup.builder()
			.questionSets(grouped)
			.build();
	}
}
