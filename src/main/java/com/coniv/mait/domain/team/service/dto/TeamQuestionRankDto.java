package com.coniv.mait.domain.team.service.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeamQuestionRankDto {
	private Long userId;
	private String name;
	private String nickname;
	private Long scorerCount;
	private Integer rank;

	public static TeamQuestionRankDto of(Long userId, String name, String nickname, Long scorerCount, Integer rank) {
		return TeamQuestionRankDto.builder()
			.userId(userId)
			.name(name)
			.nickname(nickname)
			.scorerCount(scorerCount)
			.rank(rank)
			.build();
	}
}

