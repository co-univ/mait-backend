package com.coniv.mait.domain.team.service.dto;

import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamUserDto {

	private Long id;

	private Long teamId;

	private String teamName;

	private String userName;

	private String nickname;

	private TeamUserRole role;

	public static TeamUserDto from(final TeamUserEntity teamUserEntity) {
		return TeamUserDto.builder()
			.id(teamUserEntity.getId())
			.teamId(teamUserEntity.getTeam().getId())
			.teamName(teamUserEntity.getTeam().getName())
			.userName(teamUserEntity.getUser().getName())
			.nickname(teamUserEntity.getUser().getNickname())
			.role(teamUserEntity.getUserRole())
			.build();
	}
}
