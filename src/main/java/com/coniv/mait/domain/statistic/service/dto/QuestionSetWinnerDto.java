package com.coniv.mait.domain.statistic.service.dto;

import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.user.entity.UserEntity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestionSetWinnerDto {

	@Schema(description = "우승자 사용자 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long userId;
	@Schema(description = "우승자 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	private String name;
	@Schema(description = "우승자 닉네임", requiredMode = Schema.RequiredMode.REQUIRED)
	private String nickname;

	public static QuestionSetWinnerDto from(final QuestionSetParticipantEntity participant) {
		UserEntity user = participant.getUser();
		return QuestionSetWinnerDto.builder()
			.userId(user.getId())
			.name(user.getName())
			.nickname(user.getNickname())
			.build();
	}
}
