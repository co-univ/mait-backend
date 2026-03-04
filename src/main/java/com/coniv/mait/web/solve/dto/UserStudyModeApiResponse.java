package com.coniv.mait.web.solve.dto;

import java.time.LocalDateTime;

import com.coniv.mait.domain.solve.enums.SolvingStatus;
import com.coniv.mait.domain.solve.service.dto.SolvingSessionDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserStudyModeApiResponse(
	@Schema(description = "풀이 세션 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	Long solvingSessionId,

	@Schema(description = "문제셋 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	Long questionSetId,

	@Schema(description = "풀이 상태", enumAsRef = true, requiredMode = Schema.RequiredMode.REQUIRED)
	SolvingStatus status,

	@Schema(description = "풀이 시작 시간", requiredMode = Schema.RequiredMode.REQUIRED)
	LocalDateTime startedAt
) {
	public static UserStudyModeApiResponse from(SolvingSessionDto dto) {
		return new UserStudyModeApiResponse(
			dto.getId(),
			dto.getQuestionSetId(),
			dto.getStatus(),
			dto.getStartedAt()
		);
	}
}
