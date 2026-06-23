package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.service.dto.QuestionSetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record CreateQuestionSetApiResponse(

	@Schema(description = "생성된 문제 셋의 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	Long questionSetId,

	@Schema(description = "문제 셋 제목", requiredMode = Schema.RequiredMode.REQUIRED)
	String title,

	@Deprecated
	@Schema(description = "문제 셋 제목(deprecated, title과 동일)", requiredMode = Schema.RequiredMode.REQUIRED,
		deprecated = true)
	String subject

) {
	public static CreateQuestionSetApiResponse from(QuestionSetDto questionSetDto) {
		return CreateQuestionSetApiResponse.builder()
			.questionSetId(questionSetDto.getId())
			.title(questionSetDto.getTitle())
			.subject(questionSetDto.getTitle())
			.build();
	}
}
