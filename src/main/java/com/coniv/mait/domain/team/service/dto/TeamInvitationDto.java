package com.coniv.mait.domain.team.service.dto;

import java.time.LocalDateTime;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamInvitationLinkEntity;
import com.coniv.mait.domain.team.enums.InvitationApplicationStatus;
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
public class TeamInvitationDto {

	private Long teamInviteId;

	private Long teamId;

	private Long invitorId;

	private String teamName;

	private InviteTokenDuration tokenDuration;

	private boolean requiresApproval;

	private TeamUserRole teamUserRole;

	private LocalDateTime expiredAt;

	private InvitationApplicationStatus applicationStatus;

	public static TeamInvitationDto from(final TeamInvitationLinkEntity inviteEntity, final TeamEntity teamEntity,
		final InvitationApplicationStatus applicationStatus) {
		return TeamInvitationDto.builder()
			.teamInviteId(inviteEntity.getId())
			.teamId(teamEntity.getId())
			.invitorId(inviteEntity.getInvitor().getId())
			.teamName(teamEntity.getName())
			.tokenDuration(inviteEntity.getTokenDuration())
			.requiresApproval(inviteEntity.isRequiresApproval())
			.teamUserRole(inviteEntity.getRoleOnJoin())
			.expiredAt(inviteEntity.getExpiredAt())
			.applicationStatus(applicationStatus)
			.build();
	}
}
