package com.coniv.mait.domain.team.service.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestionRanksDto {
	private List<UserRankDto> teamRank;
	private UserRankDto myRank;

	public static QuestionRanksDto of(
		List<UserRankDto> teamRank,
		UserRankDto myRank
	) {
		return QuestionRanksDto.builder()
			.teamRank(teamRank)
			.myRank(myRank)
			.build();
	}
}

