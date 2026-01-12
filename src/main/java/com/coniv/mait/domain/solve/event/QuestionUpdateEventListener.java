package com.coniv.mait.domain.solve.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.coniv.mait.domain.solve.service.QuestionSubmitRecordRegradeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionUpdateEventListener {

	private final QuestionSubmitRecordRegradeService questionSubmitRecordRegradeService;

	@Async("maitThreadPoolExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleQuestionUpdateEvent(final QuestionUpdateEvent event) {
		try {
			questionSubmitRecordRegradeService.regradeSubmitRecords(event.questionId());
		} catch (Exception e) {
			log.error("[재채점 실패] questionId={}", event.questionId(), e);
		}
	}
}
