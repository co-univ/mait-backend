package com.coniv.mait.web.admin.dto;

import com.coniv.mait.domain.solve.service.dto.FillBlankQuestionSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.MultipleQuestionSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.OrderingQuestionSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.ShortQuestionSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;
import com.coniv.mait.domain.statistic.service.dto.SubmittedAnswerGroupDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record SubmittedAnswerGroupApiResponse(
	@Schema(description = "해당 답안으로 제출한 횟수", requiredMode = Schema.RequiredMode.REQUIRED)
	long submitCount,

	@Schema(description = "정/오답 여부", requiredMode = Schema.RequiredMode.REQUIRED)
	boolean isCorrect,

	@Schema(
		description = "제출한 답안 (문제 유형에 따라 형태가 다름)",
		requiredMode = Schema.RequiredMode.REQUIRED,
		oneOf = {
			ShortQuestionSubmitAnswer.class,
			MultipleQuestionSubmitAnswer.class,
			OrderingQuestionSubmitAnswer.class,
			FillBlankQuestionSubmitAnswer.class
		})
	SubmitAnswerDto<?> submittedAnswer
) {
	public static SubmittedAnswerGroupApiResponse from(final SubmittedAnswerGroupDto group) {
		return SubmittedAnswerGroupApiResponse.builder()
			.submitCount(group.getSubmitCount())
			.isCorrect(group.isCorrect())
			.submittedAnswer(group.getSubmittedAnswer())
			.build();
	}
}
