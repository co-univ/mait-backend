package com.coniv.mait.domain.solve.event;

import com.coniv.mait.global.event.MaitEvent;

import lombok.Builder;

@Builder
public record QuestionUpdateEvent(
	Long questionId
) implements MaitEvent {
}
