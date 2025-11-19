package com.coniv.mait.web.team.dto;

import com.coniv.mait.domain.team.service.dto.TeamDto;

public record TeamApiResponse(
	Long teamId,
	String teamName
) {
	public static TeamApiResponse of(TeamDto teamDto) {
		return new TeamApiResponse(teamDto.getId(), teamDto.getName());
	}
}
