package com.coniv.mait.web.team.dto;

import com.coniv.mait.domain.team.enums.TeamUserRole;

import jakarta.validation.constraints.NotNull;

public record UpdateTeamUserRoleApiRequest(
	@NotNull
	TeamUserRole role
) {
}
