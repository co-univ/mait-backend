package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.team.service.dto.TeamQuestionRankDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TeamQuestionRankApiResponse(
	@Schema(description = "유저 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	Long userId,

	@Schema(description = "유저 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	String name,

	@Schema(description = "유저 닉네임", requiredMode = Schema.RequiredMode.REQUIRED)
	String nickname,

	@Schema(description = "점수", requiredMode = Schema.RequiredMode.REQUIRED)
	Long scorerCount,

	@Schema(description = "등수", requiredMode = Schema.RequiredMode.REQUIRED)
	Integer rank
) {
	public static TeamQuestionRankApiResponse from(TeamQuestionRankDto dto) {
		return new TeamQuestionRankApiResponse(
			dto.getUserId(),
			dto.getName(),
			dto.getNickname(),
			dto.getScorerCount(),
			dto.getRank()
		);
	}
}

