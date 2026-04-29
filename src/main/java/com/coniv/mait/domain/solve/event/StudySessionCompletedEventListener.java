package com.coniv.mait.domain.solve.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.coniv.mait.domain.question.service.StudyAutoEndService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudySessionCompletedEventListener {

	private final StudyAutoEndService studyAutoEndService;

	@Async("maitThreadPoolExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleStudySessionCompletedEvent(final StudySessionCompletedEvent event) {
		try {
			studyAutoEndService.evaluateAndAutoEnd(event.questionSetId());
		} catch (Exception e) {
			log.error("[StudyAutoEnd] 평가 실패 questionSetId={}", event.questionSetId(), e);
		}
	}
}
