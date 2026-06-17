package com.coniv.mait.domain.admin.service.dto;

import java.time.LocalDateTime;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.enums.TeamType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminTeamDto {

	private final Long teamId;
	private final String name;
	private final TeamType type;
	private final Long creatorId;
	private final LocalDateTime createdAt;

	public static AdminTeamDto from(final TeamEntity team) {
		return AdminTeamDto.builder()
			.teamId(team.getId())
			.name(team.getName())
			.type(team.getType())
			.creatorId(team.getCreatorId())
			.createdAt(team.getCreatedAt())
			.build();
	}
}
