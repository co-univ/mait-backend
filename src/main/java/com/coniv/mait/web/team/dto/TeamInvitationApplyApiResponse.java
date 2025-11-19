package com.coniv.mait.web.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TeamInvitationApplyApiResponse(
	@Schema(description = "팀 아이디", requiredMode = Schema.RequiredMode.REQUIRED)
	Long teamId,

	@Schema(description = "팀 즉시 가입되는지 유무", requiredMode = Schema.RequiredMode.REQUIRED)
	boolean joinedImmediate
) {
	public static TeamInvitationApplyApiResponse of(Long teamId, boolean joinedImmediate) {
		return new TeamInvitationApplyApiResponse(teamId, joinedImmediate);
	}
}
