package com.coniv.mait.domain.team.event;

import java.util.List;

import com.coniv.mait.global.event.MaitEvent;

import lombok.Builder;

@Builder
public record TeamMemberLeftEvent(
	String memberName,
	String teamName,
	List<String> recipientEmails
) implements MaitEvent {
}
