package com.coniv.mait.web.team.dto;

import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.service.dto.TeamApplicantDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ApplyTeamUserApiResponse(

	@Schema(description = "팀 신청 pk", requiredMode = Schema.RequiredMode.REQUIRED)
	Long id,

	@Schema(description = "유저 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	String name,

	@Schema(description = "유저 닉네임", requiredMode = Schema.RequiredMode.REQUIRED)
	String nickname,

	@Schema(description = "팀 유저 역할", requiredMode = Schema.RequiredMode.REQUIRED)
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
