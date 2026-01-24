package com.coniv.mait.web.team.dto;

import com.coniv.mait.domain.team.service.dto.TeamQuestionRankDto;

public record TeamQuestionRankApiResponse(
	Long userId,
	String name,
	String nickname,
	Long scorerCount
) {
	public static TeamQuestionRankApiResponse from(TeamQuestionRankDto dto) {
		return new TeamQuestionRankApiResponse(
			dto.getUserId(),
			dto.getName(),
			dto.getNickname(),
			dto.getScorerCount()
		);
	}
}

