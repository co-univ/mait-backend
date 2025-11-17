package com.coniv.mait.web.solve.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record QuestionAnswerSubmitRecordsApiResponse(
	@Schema(description = "전체 기록 개수", requiredMode = Schema.RequiredMode.REQUIRED)
	long totalCounts,

	@Schema(description = "정답 기록 개수", requiredMode = Schema.RequiredMode.REQUIRED)
	long correctCounts,

	@Schema(description = "오답 기록 개수", requiredMode = Schema.RequiredMode.REQUIRED)
	long incorrectCounts,
	@Schema(description = "선착순 제출 기록으로 정렬된 제출 기록 목록", requiredMode = Schema.RequiredMode.REQUIRED)
	List<QuestionAnswerSubmitRecordApiResponse> submitRecords
) {
	public static QuestionAnswerSubmitRecordsApiResponse from(
		List<QuestionAnswerSubmitRecordApiResponse> submitRecords
	) {
		long totalCounts = submitRecords.size();
		long correctCounts = submitRecords.stream().filter(QuestionAnswerSubmitRecordApiResponse::isCorrect).count();
		long incorrectCounts = totalCounts - correctCounts;

		return new QuestionAnswerSubmitRecordsApiResponse(
			totalCounts,
			correctCounts,
			incorrectCounts,
			submitRecords
		);
	}
}
