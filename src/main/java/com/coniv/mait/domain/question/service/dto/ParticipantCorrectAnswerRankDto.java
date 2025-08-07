package com.coniv.mait.domain.question.service.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ParticipantCorrectAnswerRankDto {
	List<ParticipantCorrectAnswersDto> activeParticipants;
	List<ParticipantCorrectAnswersDto> eliminatedParticipants;

	public static ParticipantCorrectAnswerRankDto of(List<ParticipantCorrectAnswersDto> activeParticipants,
		List<ParticipantCorrectAnswersDto> eliminatedParticipants) {
		return new ParticipantCorrectAnswerRankDto(activeParticipants, eliminatedParticipants);
	}
}
