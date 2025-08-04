package com.coniv.mait.web.team.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTeamApiRequest(

	@NotBlank(message = "팀 이름은 필수입니다.")
	String name
) {
}
