package com.coniv.mait.domain.question.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.coniv.mait.domain.question.service.component.QuestionWebSocketSender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewParticipantEventListener {

	private final QuestionWebSocketSender questionWebSocketSender;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleNewParticipantEvent(final NewParticipantEvent event) {
		try {
			questionWebSocketSender.broadcastNewParticipantToMaker(event.questionSetId(), event.participant());
		} catch (Exception e) {
			log.error("[새 참여자 알림 실패] questionSetId={}, userId={}", event.questionSetId(),
				event.participant().getUserId(), e);
		}
	}
}
