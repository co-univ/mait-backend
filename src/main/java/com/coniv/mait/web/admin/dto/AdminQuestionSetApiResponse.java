package com.coniv.mait.web.admin.dto;

import java.time.LocalDateTime;

import com.coniv.mait.domain.admin.service.dto.AdminQuestionSetDto;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminQuestionSetApiResponse(
	@Schema(description = "문제 셋 PK", requiredMode = Schema.RequiredMode.REQUIRED)
	Long id,

	@Schema(description = "문제 셋 제목", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	String title,

	@Schema(description = "문제 셋 주제", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	String subject,

	@Schema(description = "문제 셋 상태", requiredMode = Schema.RequiredMode.REQUIRED, enumAsRef = true)
	QuestionSetStatus status,

	@Schema(description = "풀이 방식", requiredMode = Schema.RequiredMode.NOT_REQUIRED, enumAsRef = true)
	QuestionSetSolveMode solveMode,

	@Schema(description = "팀 PK", requiredMode = Schema.RequiredMode.REQUIRED)
	Long teamId,

	@Schema(description = "생성 일시", requiredMode = Schema.RequiredMode.REQUIRED)
	LocalDateTime createdAt,

	@Schema(description = "수정 일시", requiredMode = Schema.RequiredMode.REQUIRED)
	LocalDateTime updatedAt
) {
	public static AdminQuestionSetApiResponse from(final AdminQuestionSetDto questionSet) {
		return new AdminQuestionSetApiResponse(
			questionSet.getId(),
			questionSet.getTitle(),
			questionSet.getSubject(),
			questionSet.getStatus(),
			questionSet.getSolveMode(),
			questionSet.getTeamId(),
			questionSet.getCreatedAt(),
			questionSet.getUpdatedAt());
	}
}
