package com.coniv.mait.domain.question.event;

import com.coniv.mait.domain.question.dto.ParticipantDto;
import com.coniv.mait.global.event.MaitEvent;

import lombok.Builder;

@Builder
public record NewParticipantEvent(
	Long questionSetId,
	ParticipantDto participant
) implements MaitEvent {
}
