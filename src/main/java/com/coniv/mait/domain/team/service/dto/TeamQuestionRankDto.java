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

	public static TeamQuestionRankDto of(Long userId, String name, String nickname, Long scorerCount) {
		return TeamQuestionRankDto.builder()
			.userId(userId)
			.name(name)
			.nickname(nickname)
			.scorerCount(scorerCount)
			.build();
	}
}

