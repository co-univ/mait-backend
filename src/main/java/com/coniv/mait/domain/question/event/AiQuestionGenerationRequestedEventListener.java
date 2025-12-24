package com.coniv.mait.domain.question.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.coniv.mait.domain.question.service.QuestionService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AiQuestionGenerationRequestedEventListener {

	private final QuestionService questionService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void handle(final AiQuestionGenerationRequestedEvent event) {
		questionService.createAiGeneratedQuestions(
			event.questionSetId(),
			event.counts(),
			event.materials(),
			event.instruction(),
			event.difficulty());
	}
}
