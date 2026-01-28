package com.coniv.mait.web.team.dto;

import java.util.List;

import com.coniv.mait.domain.team.service.dto.TeamQuestionRankCombinedDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TeamQuestionRankCombinedApiResponse(
	@Schema(description = "선착순 랭킹", requiredMode = Schema.RequiredMode.REQUIRED)
	List<TeamQuestionRankApiResponse> scorerRank,

	@Schema(description = "정답자 랭킹", requiredMode = Schema.RequiredMode.REQUIRED)
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

