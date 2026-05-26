package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.solve.service.dto.RankDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record QuestionSetScorerRankApiResponse(
	@Schema(description = "문제 셋 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	Long questionSetId,

	@Schema(description = "득점자 전체 랭킹(요청한 rankCount 상위만 노출)", requiredMode = Schema.RequiredMode.REQUIRED)
	List<RankDto> rankings,

	@Schema(description = "내 랭킹", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	RankDto userRank,

	@Schema(description = "요청한 rankCount 범위 내에 내 랭킹(userRank)이 포함되는지 여부", requiredMode = Schema.RequiredMode.REQUIRED)
	boolean containsUserRank
) {

	public static QuestionSetScorerRankApiResponse of(List<RankDto> ranks, Long questionSetId, Long userId,
		int rankCount) {
		RankDto userRank = ranks.stream()
			.filter(rank -> rank.getUser().getId().equals(userId))
			.findFirst()
			.orElse(null);
		boolean containsUserRank = userRank != null && userRank.getRank() <= rankCount;
		List<RankDto> limitedRank = ranks.stream()
			.filter(rank -> rank.getRank() <= rankCount)
			.toList();
		return QuestionSetScorerRankApiResponse.builder()
			.questionSetId(questionSetId)
			.rankings(limitedRank)
			.userRank(userRank)
			.containsUserRank(containsUserRank)
			.build();
	}
}
