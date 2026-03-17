package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.solve.service.dto.RankDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record TeamRankApiResponse(
	@Schema(description = "조회한 랭킹 타입", enumAsRef = true, requiredMode = Schema.RequiredMode.REQUIRED)
	RankType type,

	@Schema(description = "조회하는 팀 PK", requiredMode = Schema.RequiredMode.REQUIRED)
	Long teamId,
	@Schema(description = "전체 랭킹", requiredMode = Schema.RequiredMode.REQUIRED)
	List<RankDto> teamRankings,

	@Schema(description = "내 랭킹", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	RankDto userRank,

	@Schema(description = "요청한 rankCount 범위 내에 내 랭킹(userRank)이 포함되는지 여부", requiredMode = Schema.RequiredMode.REQUIRED)
	boolean containsUserRank
) {

	public static TeamRankApiResponse of(List<RankDto> ranks, Long teamId, Long userId, int rankCount, RankType type) {
		RankDto userRank = ranks.stream()
			.filter(rank -> rank.getUser().getId().equals(userId))
			.findFirst()
			.orElse(null);
		boolean containsUserRank = userRank != null && userRank.getRank() <= rankCount;
		List<RankDto> limitedRank = ranks.stream()
			.filter(rank -> rank.getRank() <= rankCount)
			.toList();
		return TeamRankApiResponse.builder()
			.type(type)
			.teamId(teamId)
			.teamRankings(limitedRank)
			.userRank(userRank)
			.containsUserRank(containsUserRank)
			.build();
	}

	public enum RankType {
		SCORER,
		CORRECT
	}
}
