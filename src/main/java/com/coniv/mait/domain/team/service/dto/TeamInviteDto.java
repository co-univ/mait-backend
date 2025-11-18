package com.coniv.mait.domain.team.service.dto;

import java.time.LocalDateTime;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamInviteEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.global.enums.InviteTokenDuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamInviteDto {

	private Long teamInviteId;

	private Long teamId;

	private Long invitorId;

	private String teamName;

	private InviteTokenDuration tokenDuration;

	private boolean requiresApproval;

	private TeamUserRole teamUserRole;

	private LocalDateTime expiredAt;

	private boolean isExpired;

	public static TeamInviteDto from(final TeamInviteEntity inviteEntity, final TeamEntity teamEntity,
		final boolean isExpired) {
		return TeamInviteDto.builder()
			.teamInviteId(inviteEntity.getId())
			.teamId(teamEntity.getId())
			.invitorId(teamEntity.getId())
			.teamName(teamEntity.getName())
			.tokenDuration(inviteEntity.getTokenDuration())
			.requiresApproval(inviteEntity.isRequiresApproval())
			.teamUserRole(inviteEntity.getRoleOnJoin())
			.expiredAt(inviteEntity.getExpiredAt())
			.isExpired(isExpired)
			.build();
	}
}
