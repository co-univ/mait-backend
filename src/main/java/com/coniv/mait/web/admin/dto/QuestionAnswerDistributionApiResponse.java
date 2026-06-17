package com.coniv.mait.web.admin.dto;

import java.util.List;

import com.coniv.mait.domain.statistic.service.dto.QuestionAnswerDistributionDto;
import com.coniv.mait.web.question.dto.QuestionApiResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record QuestionAnswerDistributionApiResponse(
	@Schema(description = "문제 셋 PK", requiredMode = Schema.RequiredMode.REQUIRED)
	Long questionSetId,

	@Schema(description = "문제 정보 (PK, 내용, 해설, 실제 정답 포함)", requiredMode = Schema.RequiredMode.REQUIRED)
	QuestionApiResponse question,

	@Schema(description = "해당 문제의 전체 제출 수", requiredMode = Schema.RequiredMode.REQUIRED)
	long totalSubmitCount,

	@Schema(description = "제출한 유저 수 (유저당 최초 제출 기준)", requiredMode = Schema.RequiredMode.REQUIRED)
	long submittedUserCount,

	@Schema(description = "정답 유저 수 (유저당 최초 제출 기준)", requiredMode = Schema.RequiredMode.REQUIRED)
	long correctUserCount,

	@Schema(description = "정답률(%) (유저당 최초 제출 기준, 제출자가 없으면 null)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	Double correctRate,

	@Schema(description = "동일 답안끼리 그룹화한 제출 통계 목록 (제출 수 내림차순)", requiredMode = Schema.RequiredMode.REQUIRED)
	List<SubmittedAnswerGroupApiResponse> groups
) {
	public static QuestionAnswerDistributionApiResponse from(final QuestionAnswerDistributionDto distribution) {
		return QuestionAnswerDistributionApiResponse.builder()
			.questionSetId(distribution.getQuestionSetId())
			.question(QuestionApiResponse.from(distribution.getQuestion()))
			.totalSubmitCount(distribution.getTotalSubmitCount())
			.submittedUserCount(distribution.getSubmittedUserCount())
			.correctUserCount(distribution.getCorrectUserCount())
			.correctRate(distribution.getCorrectRate())
			.groups(distribution.getGroups().stream()
				.map(SubmittedAnswerGroupApiResponse::from)
				.toList())
			.build();
	}
}
