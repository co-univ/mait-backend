package com.coniv.mait.web.team.dto;

import com.coniv.mait.domain.team.enums.TeamUserRole;

import jakarta.validation.constraints.NotNull;

public record AddTeamUserApiRequest(
	@NotNull(message = "사용자 ID는 필수입니다.")
	Long userId,

	@NotNull(message = "역할은 필수입니다.")
	TeamUserRole role
) {
}

