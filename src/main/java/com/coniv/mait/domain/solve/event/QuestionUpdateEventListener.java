package com.coniv.mait.domain.solve.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.coniv.mait.domain.solve.service.QuestionAnswerSubmitService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QuestionUpdateEventListener {

	private final QuestionAnswerSubmitService questionAnswerSubmitService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleQuestionUpdateEvent(final QuestionUpdateEvent event) {
		questionAnswerSubmitService.regradeSubmitRecords(event.questionId());
	}
}
