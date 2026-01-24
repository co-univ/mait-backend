package com.coniv.mait.web.team.dto;

import java.util.List;

import com.coniv.mait.domain.team.service.dto.TeamQuestionRankCombinedDto;

public record TeamQuestionRankCombinedApiResponse(
	List<TeamQuestionRankApiResponse> scorerRank,
	List<TeamQuestionRankApiResponse> correctAnswerRank
) {
	public static TeamQuestionRankCombinedApiResponse from(
		TeamQuestionRankCombinedDto combinedDto
	) {
		List<TeamQuestionRankApiResponse> scorerRank = combinedDto.getScorerRank().stream()
			.map(TeamQuestionRankApiResponse::from)
			.toList();

		List<TeamQuestionRankApiResponse> correctAnswerRank = combinedDto.getCorrectAnswerRank().stream()
			.map(TeamQuestionRankApiResponse::from)
			.toList();

		return new TeamQuestionRankCombinedApiResponse(scorerRank, correctAnswerRank);
	}
}

