package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.question.enums.ParticipantStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "현재 유저의 실시간 문제 셋 참여 상태")
public enum UserParticipationStatus {
	NOT_PARTICIPATED("참여 전"),
	PARTICIPATING("참가중"),
	ELIMINATED("탈락"),
	FINISHED("종료");

	private final String description;

	public static UserParticipationStatus fromOngoingQuestionSet(final QuestionSetParticipantEntity participant) {
		if (participant == null) {
			return NOT_PARTICIPATED;
		}
		if (participant.getStatus() == ParticipantStatus.ELIMINATED) {
			return ELIMINATED;
		}
		return PARTICIPATING;
	}
}
