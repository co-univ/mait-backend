package com.coniv.mait.domain.question.service.component;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LiveParticipantBroadcaster {

	private final LiveParticipantRedisRepository liveParticipantRedisRepository;
	private final QuestionWebSocketSender questionWebSocketSender;

	public void broadcastCount(final Long questionSetId) {
		long count = liveParticipantRedisRepository.getParticipantCount(questionSetId);
		questionWebSocketSender.broadcastParticipantCount(questionSetId, count);
	}
}
