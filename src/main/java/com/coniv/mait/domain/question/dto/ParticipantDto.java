package com.coniv.mait.domain.question.dto;

import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ParticipantDto {
	Long participantId;
	Long userId;
	String participantName;

	public static ParticipantDto from(QuestionSetParticipantEntity participant) {
		return new ParticipantDto(participant.getId(), participant.getUser().getId(), participant.getParticipantName());
	}
}
