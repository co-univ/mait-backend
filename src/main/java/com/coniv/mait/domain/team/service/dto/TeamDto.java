package com.coniv.mait.domain.team.service.dto;

import com.coniv.mait.domain.team.entity.TeamEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamDto {

	private Long id;

	private String name;

	public static TeamDto from(final TeamEntity teamEntity) {
		return TeamDto.builder()
			.id(teamEntity.getId())
			.name(teamEntity.getName())
			.build();
	}
}
