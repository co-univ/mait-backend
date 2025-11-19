package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.dto.AnswerRankDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record ScorerRanksApiResponse(
	@Schema(description = "문제 셋 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	Long questionSetId,

	@Schema(description = "득점자 그룹")
	List<ScorerRankApiResponse> ranksGroup
) {

	@Builder
	record ScorerRankApiResponse(
		@Schema(description = "득점한 문제 개수", requiredMode = Schema.RequiredMode.REQUIRED)
		long scoreCount,

		@Schema(description = "유저 정보")
		List<CorrectorRanksApiResponse.UserApiResponse> users
	) {
		static ScorerRankApiResponse from(AnswerRankDto scorerRanks) {
			return ScorerRankApiResponse.builder()
				.scoreCount(scorerRanks.getCount())
				.users(scorerRanks.getUsers().stream().map(CorrectorRanksApiResponse.UserApiResponse::from).toList())
				.build();
		}
	}

	public static ScorerRanksApiResponse of(final Long questionSetId, final List<AnswerRankDto> scorerRanks) {
		return ScorerRanksApiResponse.builder()
			.questionSetId(questionSetId)
			.ranksGroup(scorerRanks.stream().map(ScorerRankApiResponse::from).toList())
			.build();
	}
}
