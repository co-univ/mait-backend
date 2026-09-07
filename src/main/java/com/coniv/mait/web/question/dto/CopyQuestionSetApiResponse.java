package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.service.dto.QuestionSetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record CopyQuestionSetApiResponse(

	@Schema(description = "생성된 복제본 문제 셋의 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	Long questionSetId,

	@Schema(description = "복제본 문제 셋 제목", requiredMode = Schema.RequiredMode.REQUIRED)
	String title,

	@Schema(description = "복제본이 생성된 팀 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	Long teamId

) {
	public static CopyQuestionSetApiResponse from(final QuestionSetDto questionSetDto) {
		return CopyQuestionSetApiResponse.builder()
			.questionSetId(questionSetDto.getId())
			.title(questionSetDto.getTitle())
			.teamId(questionSetDto.getTeamId())
			.build();
	}
}
