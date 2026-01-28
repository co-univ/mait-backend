package com.coniv.mait.domain.team.service.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeamQuestionRankCombinedDto {
	private List<TeamQuestionRankDto> teamRank;
	private TeamQuestionRankDto myRank;

	public static TeamQuestionRankCombinedDto of(
		List<TeamQuestionRankDto> teamRank,
		TeamQuestionRankDto myRank
	) {
		return TeamQuestionRankCombinedDto.builder()
			.teamRank(teamRank)
			.myRank(myRank)
			.build();
	}
}

