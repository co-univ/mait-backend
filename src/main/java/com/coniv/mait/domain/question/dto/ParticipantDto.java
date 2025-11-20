package com.coniv.mait.domain.question.dto;

import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.question.enums.ParticipantStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ParticipantDto {
	Long participantId;
	Long userId;
	String participantName;
	String userNickname;
	boolean winner;
	ParticipantStatus status;

	public static ParticipantDto from(QuestionSetParticipantEntity participant) {
		return ParticipantDto.builder()
			.participantId(participant.getId())
			.userId(participant.getUser().getId())
			.participantName(participant.getParticipantName())
			.status(participant.getStatus())
			.userNickname(participant.getUser().getNickname())
			.winner(participant.isWinner())
			.build();
	}
}
