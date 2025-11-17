package com.coniv.mait.web.solve.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record QuestionAnswerSubmitRecordsApiResponse(
	@Schema(description = "전체 기록 개수", requiredMode = Schema.RequiredMode.REQUIRED)
	long totalCounts,

	@Schema(description = "정답자 수", requiredMode = Schema.RequiredMode.REQUIRED)
	long correctUserCounts,

	@Schema(description = "오답자 수", requiredMode = Schema.RequiredMode.REQUIRED)
	long incorrectUserCounts,
	@Schema(description = "선착순 제출 기록으로 정렬된 제출 기록 목록", requiredMode = Schema.RequiredMode.REQUIRED)
	List<QuestionAnswerSubmitRecordApiResponse> submitRecords
) {
	public static QuestionAnswerSubmitRecordsApiResponse from(
		List<QuestionAnswerSubmitRecordApiResponse> submitRecords
	) {
		long totalCounts = submitRecords.size();
		long correctUserCounts = submitRecords.stream()
			.filter(QuestionAnswerSubmitRecordApiResponse::isCorrect)
			.count();
		long incorrectUserCounts = submitRecords.stream().filter(record -> !record.isCorrect())
			.map(QuestionAnswerSubmitRecordApiResponse::userId).distinct().count();

		return new QuestionAnswerSubmitRecordsApiResponse(
			totalCounts,
			correctUserCounts,
			incorrectUserCounts,
			submitRecords
		);
	}
}
