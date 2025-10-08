package com.coniv.mait.web.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record CreateTeamInviteApiResponse(
	@Schema(description = "팀 초대 토큰", requiredMode = Schema.RequiredMode.REQUIRED)
	String token
) {
	public static CreateTeamInviteApiResponse from(String token) {
		return CreateTeamInviteApiResponse.builder()
			.token(token)
			.build();
	}
}
