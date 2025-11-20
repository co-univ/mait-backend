package com.coniv.mait.domain.team.service.dto;

import java.time.LocalDateTime;

import com.coniv.mait.domain.team.entity.TeamInvitationLinkEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamInvitationLinkDto {
	private Long teamInvitationLinkId;

	private TeamUserRole roleOnJoin;

	private String token;

	private LocalDateTime expiredAt;

	public static TeamInvitationLinkDto from(final TeamInvitationLinkEntity teamInvitationLink) {
		return TeamInvitationLinkDto.builder()
			.teamInvitationLinkId(teamInvitationLink.getId())
			.roleOnJoin(teamInvitationLink.getRoleOnJoin())
			.token(teamInvitationLink.getToken())
			.expiredAt(teamInvitationLink.getExpiredAt())
			.build();
	}
}
