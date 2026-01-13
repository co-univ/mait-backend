package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.service.dto.GradedAnswerResult;
import com.coniv.mait.domain.question.service.dto.ReviewAnswerCheckResult;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record QuestionAnswerReviewSubmitApiResponse(
	@Schema(description = "유저가 제출한 문제 PK", requiredMode = Schema.RequiredMode.REQUIRED)
	Long questionId,

	@Schema(description = "유저가 제출한 문제 정답 여부", requiredMode = Schema.RequiredMode.REQUIRED)
	Boolean isCorrect,

	@Schema(description = "문제 유형")
	QuestionType type,

	@Schema(description = "제출한 정답에 대한 채점 결과")
	List<? extends GradedAnswerResult> gradedResults
) {

	public static QuestionAnswerReviewSubmitApiResponse from(ReviewAnswerCheckResult result) {
		return QuestionAnswerReviewSubmitApiResponse.builder()
			.questionId(result.questionId())
			.isCorrect(result.isCorrect())
			.type(result.type())
			.gradedResults(result.gradedResults())
			.build();
	}
}
