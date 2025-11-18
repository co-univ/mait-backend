package com.coniv.mait.web.team.dto;

import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.global.enums.InviteTokenDuration;

import jakarta.validation.constraints.NotNull;

public record CreateTeamInviteApiRequest(
	@NotNull(message = "유지 기간은 필수입니다.")
	InviteTokenDuration duration,

	@NotNull(message = "권한 유형은 필수입니다.")
	TeamUserRole role
) {
}
