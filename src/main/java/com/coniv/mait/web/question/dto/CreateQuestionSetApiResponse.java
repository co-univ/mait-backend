package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.service.dto.QuestionSetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record CreateQuestionSetApiResponse(

	@Schema(description = "생성된 문제 셋의 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	Long questionSetId,

	@Schema(description = "문제 셋 주제", requiredMode = Schema.RequiredMode.REQUIRED)
	String subject

) {
	public static CreateQuestionSetApiResponse from(QuestionSetDto questionSetDto) {
		return CreateQuestionSetApiResponse.builder()
			.questionSetId(questionSetDto.getId())
			.subject(questionSetDto.getSubject())
			.build();
	}
}
