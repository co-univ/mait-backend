package com.coniv.mait.web.solve.dto;

import java.util.List;

import com.coniv.mait.domain.solve.service.dto.AnswerSubmitDto;
import com.coniv.mait.domain.solve.service.dto.StudyGradeResultDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record StudyGradeResultApiResponse(
	@Schema(description = "문제 셋 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	Long questionSetId,

	@Schema(description = "풀이 세션 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	Long solvingSessionId,

	@Schema(description = "총 문제 수", requiredMode = Schema.RequiredMode.REQUIRED)
	int totalCount,

	@Schema(description = "정답 수", requiredMode = Schema.RequiredMode.REQUIRED)
	int correctCount,

	@Schema(description = "문제별 채점 결과", requiredMode = Schema.RequiredMode.REQUIRED)
	List<QuestionAnswerSubmitApiResponse> results
) {

	public static StudyGradeResultApiResponse from(StudyGradeResultDto dto) {
		List<QuestionAnswerSubmitApiResponse> results = dto.getResults().stream()
			.map(QuestionAnswerSubmitApiResponse::from)
			.toList();

		return StudyGradeResultApiResponse.builder()
			.questionSetId(dto.getQuestionSetId())
			.solvingSessionId(dto.getSolvingSessionId())
			.totalCount(dto.getTotalCount())
			.correctCount(dto.getCorrectCount())
			.results(results)
			.build();
	}
}
