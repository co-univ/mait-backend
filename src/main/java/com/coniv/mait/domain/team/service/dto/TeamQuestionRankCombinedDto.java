package com.coniv.mait.domain.team.service.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeamQuestionRankCombinedDto {
	private List<TeamQuestionRankDto> scorerRank;
	private List<TeamQuestionRankDto> correctAnswerRank;

	public static TeamQuestionRankCombinedDto of(
		List<TeamQuestionRankDto> scorerRank,
		List<TeamQuestionRankDto> correctAnswerRank
	) {
		return TeamQuestionRankCombinedDto.builder()
			.scorerRank(scorerRank)
			.correctAnswerRank(correctAnswerRank)
			.build();
	}
}

