package com.coniv.mait.web.admin.dto;

import java.time.LocalDateTime;

import com.coniv.mait.domain.admin.service.dto.AdminTeamDto;
import com.coniv.mait.domain.team.enums.TeamType;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminTeamApiResponse(
	@Schema(description = "팀 PK", requiredMode = Schema.RequiredMode.REQUIRED)
	Long teamId,

	@Schema(description = "팀 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	String name,

	@Schema(description = "팀 타입 (개인 워크스페이스 / 단체 팀)", requiredMode = Schema.RequiredMode.REQUIRED, enumAsRef = true)
	TeamType type,

	@Schema(description = "팀 생성자 PK", requiredMode = Schema.RequiredMode.REQUIRED)
	Long creatorId,

	@Schema(description = "팀 생성 일시", requiredMode = Schema.RequiredMode.REQUIRED)
	LocalDateTime createdAt
) {
	public static AdminTeamApiResponse from(final AdminTeamDto team) {
		return new AdminTeamApiResponse(
			team.getTeamId(),
			team.getName(),
			team.getType(),
			team.getCreatorId(),
			team.getCreatedAt());
	}
}
