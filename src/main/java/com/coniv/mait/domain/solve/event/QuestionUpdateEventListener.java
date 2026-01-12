package com.coniv.mait.domain.solve.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.coniv.mait.domain.solve.service.QuestionSubmitRecordRegradeService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QuestionUpdateEventListener {

	private final QuestionSubmitRecordRegradeService questionSubmitRecordRegradeService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleQuestionUpdateEvent(final QuestionUpdateEvent event) {
		questionSubmitRecordRegradeService.regradeSubmitRecords(event.questionId());
	}
}
