package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.dto.ParticipantDto;

public record UpdateActiveParticipantsRequest(
	List<ParticipantDto> activeParticipants,
	List<ParticipantDto> eliminatedParticipants
) {
}
