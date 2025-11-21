package com.coniv.mait.web.team.dto;

import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.service.dto.TeamUserDto;

public record TeamApiResponse(
	Long teamId,
	String teamName,
	TeamUserRole role
) {
	public static TeamApiResponse from(TeamUserDto teamUserDto) {
		return new TeamApiResponse(teamUserDto.getTeamId(), teamUserDto.getTeamName(), teamUserDto.getRole());
	}
}
