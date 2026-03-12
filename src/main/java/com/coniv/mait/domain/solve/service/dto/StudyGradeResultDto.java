package com.coniv.mait.domain.solve.service.dto;

import java.util.List;

import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudyGradeResultDto {

	private Long questionSetId;
	private Long solvingSessionId;
	private int totalCount;
	private int correctCount;
	private List<AnswerSubmitDto> results;

	public static StudyGradeResultDto of(SolvingSessionEntity solvingSession, List<AnswerSubmitRecordEntity> records) {
		List<AnswerSubmitDto> results = records.stream()
			.map(AnswerSubmitDto::from)
			.toList();

		int correctCount = (int)records.stream()
			.filter(AnswerSubmitRecordEntity::isCorrect)
			.count();

		return StudyGradeResultDto.builder()
			.questionSetId(solvingSession.getQuestionSet().getId())
			.solvingSessionId(solvingSession.getId())
			.totalCount(records.size())
			.correctCount(correctCount)
			.results(results)
			.build();
	}
}
