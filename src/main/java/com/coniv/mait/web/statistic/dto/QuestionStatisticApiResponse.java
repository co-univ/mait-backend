package com.coniv.mait.web.statistic.dto;

import com.coniv.mait.domain.statistic.service.dto.QuestionStatisticDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record QuestionStatisticApiResponse(
	@Schema(description = "문제 PK", requiredMode = Schema.RequiredMode.REQUIRED)
	Long questionId,

	@Schema(description = "문제 번호", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	Long questionNumber,

	@Schema(description = "해당 문제를 제출한 유저 수(오답률 분모)", requiredMode = Schema.RequiredMode.REQUIRED)
	long submittedUserCount,

	@Schema(description = "최초 제출이 오답인 유저 수(오답률 분자)", requiredMode = Schema.RequiredMode.REQUIRED)
	long firstWrongUserCount,

	@Schema(description = "최초 제출 기준 오답률(%). 제출자가 없으면 null", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	Double wrongRate
) {

	public static QuestionStatisticApiResponse from(final QuestionStatisticDto dto) {
		return QuestionStatisticApiResponse.builder()
			.questionId(dto.getQuestionId())
			.questionNumber(dto.getQuestionNumber())
			.submittedUserCount(dto.getSubmittedUserCount())
			.firstWrongUserCount(dto.getFirstWrongUserCount())
			.wrongRate(dto.getWrongRate())
			.build();
	}
}
