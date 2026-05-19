package com.coniv.mait.web.team.dto;

import com.coniv.mait.domain.team.enums.TeamType;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.service.dto.TeamUserDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TeamApiResponse(
	@Schema(description = "팀 pk", requiredMode = Schema.RequiredMode.REQUIRED)
	Long teamId,

	@Schema(description = "팀 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	String teamName,

	@Schema(description = "팀 타입 (개인 워크스페이스 / 단체 팀)", requiredMode = Schema.RequiredMode.REQUIRED)
	TeamType teamType,

	@Schema(description = "팀 내 유저 역할", requiredMode = Schema.RequiredMode.REQUIRED)
	TeamUserRole role
) {
	public static TeamApiResponse from(TeamUserDto teamUserDto) {
		return new TeamApiResponse(
			teamUserDto.getTeamId(),
			teamUserDto.getTeamName(),
			teamUserDto.getTeamType(),
			teamUserDto.getRole()
		);
	}
}
