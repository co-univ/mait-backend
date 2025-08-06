package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.dto.ParticipantDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ParticipantInfoResponse(
	@Schema(description = "진출 유저 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	Long userId,
	@Schema(description = "참여자 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	Long participantId,
	@Schema(description = "참여자 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	String participantName) {

	public static ParticipantInfoResponse from(ParticipantDto participant) {
		return new ParticipantInfoResponse(participant.getUserId(), participant.getParticipantId(),
			participant.getParticipantName());
	}
}
