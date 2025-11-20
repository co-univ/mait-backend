package com.coniv.mait.web.team.dto;

import java.time.LocalDateTime;

import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.service.dto.TeamInvitationLinkDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TeamInvitationLinksApiResponse(
	@Schema(description = "팀 링크 아이디", requiredMode = Schema.RequiredMode.REQUIRED)
	Long linkId,

	@Schema(description = "팀 링크", requiredMode = Schema.RequiredMode.REQUIRED)
	String token,

	@Schema(description = "팀 유저 역할", requiredMode = Schema.RequiredMode.REQUIRED)
	TeamUserRole role,

	@Schema(description = "팀 초대 만료 일시", requiredMode = Schema.RequiredMode.REQUIRED)
	LocalDateTime expiredAt
) {
	public static TeamInvitationLinksApiResponse from(TeamInvitationLinkDto dto) {
		return new TeamInvitationLinksApiResponse(
			dto.getTeamInvitationLinkId(),
			dto.getToken(),
			dto.getRoleOnJoin(),
			dto.getExpiredAt()
		);
	}
}
