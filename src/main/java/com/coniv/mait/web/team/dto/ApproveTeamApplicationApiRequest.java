package com.coniv.mait.web.team.dto;

import com.coniv.mait.domain.team.enums.InvitationApplicationStatus;

import jakarta.validation.constraints.NotNull;

public record ApproveTeamApplicationApiRequest(

	@NotNull(message = "바꿀 상태는 필수입니다.")
	InvitationApplicationStatus status
) {
}
