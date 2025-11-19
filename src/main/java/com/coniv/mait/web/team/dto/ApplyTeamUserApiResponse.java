package com.coniv.mait.web.team.dto;

import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.service.dto.TeamApplicantDto;

public record ApplyTeamUserApiResponse(
	Long id,

	String name,

	String nickname,

	TeamUserRole role
) {
	public static ApplyTeamUserApiResponse from(final TeamApplicantDto applicationDto) {
		return new ApplyTeamUserApiResponse(
			applicationDto.getApplicantId(),
			applicationDto.getName(),
			applicationDto.getNickname(),
			applicationDto.getRole()
		);
	}
}
