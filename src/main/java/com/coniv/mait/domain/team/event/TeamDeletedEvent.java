package com.coniv.mait.domain.team.event;

import java.util.List;

import com.coniv.mait.global.event.MaitEvent;

import lombok.Builder;

@Builder
public record TeamDeletedEvent(
	Long teamId,
	String teamName,
	List<MemberEmailInfo> recipients,
	List<Long> ongoingLiveQuestionSetIds
) implements MaitEvent {
}
