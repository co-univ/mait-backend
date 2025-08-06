package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.dto.ParticipantDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ActiveParticipantsResponse(
	@Schema(description = "참가자 정보", requiredMode = Schema.RequiredMode.REQUIRED)
	List<ParticipantInfoResponse> activeParticipants) {

	public static ActiveParticipantsResponse from(List<ParticipantDto> participants) {
		return new ActiveParticipantsResponse(
			participants.stream()
				.map(ParticipantInfoResponse::from)
				.toList());
	}
}
