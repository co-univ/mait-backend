package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.team.service.dto.QuestionRanksDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TeamQuestionRankCombinedApiResponse(
	@Schema(description = "전체 랭킹", requiredMode = Schema.RequiredMode.REQUIRED)
	List<TeamQuestionRankApiResponse> scorerRank,

	@Schema(description = "내 랭킹", requiredMode = Schema.RequiredMode.REQUIRED)
	TeamQuestionRankApiResponse myRank
) {
	public static TeamQuestionRankCombinedApiResponse from(
		QuestionRanksDto combinedDto
	) {
		List<TeamQuestionRankApiResponse> teamRank = combinedDto.getTeamRank().stream()
			.map(TeamQuestionRankApiResponse::from)
			.toList();

		TeamQuestionRankApiResponse myRank = combinedDto.getMyRank() != null
			? TeamQuestionRankApiResponse.from(combinedDto.getMyRank())
			: null;

		return new TeamQuestionRankCombinedApiResponse(teamRank, myRank);
	}
}

