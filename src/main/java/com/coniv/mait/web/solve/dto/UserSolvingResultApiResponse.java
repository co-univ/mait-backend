package com.coniv.mait.web.solve.dto;

import java.util.List;

import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.solve.service.dto.MySolveRecordDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record UserSolvingResultApiResponse(
	@Schema(description = "문제 셋 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	Long questionSetId,

	@Schema(description = "풀이 모드", requiredMode = Schema.RequiredMode.REQUIRED)
	QuestionSetSolveMode solveMode,

	@Schema(description = "전체 문제 수", requiredMode = Schema.RequiredMode.REQUIRED)
	int totalCount,

	@Schema(description = "맞춘 문제 수", requiredMode = Schema.RequiredMode.REQUIRED)
	int correctCount,

	@Schema(description = "100점 만점 기준 점수 (소수 첫째 자리까지)", requiredMode = Schema.RequiredMode.REQUIRED)
	double score,

	@Schema(description = "문제별 풀이 상세 (전 문제, 미응답 포함)", requiredMode = Schema.RequiredMode.REQUIRED)
	List<QuestionSolveResultApiResponse> results
) {

	public static UserSolvingResultApiResponse from(final MySolveRecordDto dto) {
		List<QuestionSolveResultApiResponse> results = dto.getResults().stream()
			.map(QuestionSolveResultApiResponse::from)
			.toList();

		return UserSolvingResultApiResponse.builder()
			.questionSetId(dto.getQuestionSetId())
			.solveMode(dto.getSolveMode())
			.totalCount(dto.getTotalCount())
			.correctCount(dto.getCorrectCount())
			.score(dto.getScore())
			.results(results)
			.build();
	}
}
