package com.coniv.mait.web.team.dto;

import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.service.dto.TeamUserDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record JoinedTeamUserApiResponse(
	@Schema(description = "팀유저 pk", requiredMode = Schema.RequiredMode.REQUIRED)
	Long teamUserId,

	@Schema(description = "유저 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	String name,

	@Schema(description = "유저 닉네임", requiredMode = Schema.RequiredMode.REQUIRED)
	String nickname,

	@Schema(description = "팀 내 유저 역할", requiredMode = Schema.RequiredMode.REQUIRED)
	TeamUserRole role
) {
	public static JoinedTeamUserApiResponse from(TeamUserDto teamUserDto) {
		return new JoinedTeamUserApiResponse(
			teamUserDto.getId(),
			teamUserDto.getName(),
			teamUserDto.getNickname(),
			teamUserDto.getRole()
		);
	}
}
