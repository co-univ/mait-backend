package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.service.dto.ParticipantCorrectAnswerRankDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantsCorrectAnswerRankResponse {
	private List<ParticipantCorrectAnswerResponse> activeParticipants;
	private List<ParticipantCorrectAnswerResponse> eliminatedParticipants;

	public static ParticipantsCorrectAnswerRankResponse from(
		ParticipantCorrectAnswerRankDto dto) {
		List<ParticipantCorrectAnswerResponse> active = dto.getActiveParticipants().stream()
			.map(ParticipantCorrectAnswerResponse::from)
			.toList();

		List<ParticipantCorrectAnswerResponse> eliminated = dto.getEliminatedParticipants().stream()
			.map(ParticipantCorrectAnswerResponse::from)
			.toList();

		return new ParticipantsCorrectAnswerRankResponse(active, eliminated);
	}
}
