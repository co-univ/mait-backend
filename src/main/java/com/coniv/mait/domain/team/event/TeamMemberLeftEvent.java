package com.coniv.mait.domain.team.event;

import com.coniv.mait.global.event.MaitEvent;

import lombok.Builder;

@Builder
public record TeamMemberLeftEvent(
	String memberName,
	String teamName,
	String memberEmail,
	String ownerEmail
) implements MaitEvent {
}
