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

	@Schema(description = "내 랭킹", requiredMode = Schema.RequiredMode.REQUIRED)
	RankDto userRank
) {

	public static TeamRankApiResponse of(List<RankDto> ranks, Long teamId, Long userId, int rankCount, RankType type) {
		RankDto userRank = ranks.stream()
			.filter(rank -> rank.getUser().getId().equals(userId))
			.findFirst()
			.orElse(null);
		List<RankDto> limitedRank = ranks.stream()
			.limit(rankCount)
			.toList();
		return TeamRankApiResponse.builder()
			.type(type)
			.teamId(teamId)
			.teamRankings(limitedRank)
			.userRank(userRank)
			.build();
	}

	public enum RankType {
		SCORER,
		CORRECT
	}
}

