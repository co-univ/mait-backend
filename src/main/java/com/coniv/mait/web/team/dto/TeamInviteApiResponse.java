package com.coniv.mait.web.team.dto;

import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.service.dto.TeamInviteDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record TeamInviteApiResponse(
	@Schema(description = "팀 초대 토큰", requiredMode = Schema.RequiredMode.REQUIRED)
	Long teamId,

	@Schema(description = "팀 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	String teamName,

	@Schema(description = "팀 초대 유형", requiredMode = Schema.RequiredMode.REQUIRED)
	TeamUserRole role,

	@Schema(description = "만료 여부", requiredMode = Schema.RequiredMode.REQUIRED)
	boolean isExpired,

	@Schema(description = "승인 필요 여부", requiredMode = Schema.RequiredMode.REQUIRED)
	boolean requiresApproval

) {
	public static TeamInviteApiResponse from(TeamInviteDto dto) {
		return TeamInviteApiResponse.builder()
			.teamId(dto.getTeamId())
			.teamName(dto.getTeamName())
			.role(dto.getTeamUserRole())
			.isExpired(dto.isExpired())
			.requiresApproval(dto.isRequiresApproval())
			.build();
	}
}
