package com.coniv.mait.domain.question.service;

import org.springframework.stereotype.Service;

import com.coniv.mait.domain.question.dto.QuestionStatusMessage;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionStatusType;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.web.question.controller.QuestionWebSocketController;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionControlService {

	private final QuestionWebSocketController questionWebSocketController;
	private final QuestionEntityRepository questionEntityRepository;

	/**
	 * 특정 문제의 접근을 허용
	 */
	public void allowQuestionAccess(Long questionSetId, Long questionId) {
		QuestionEntity question = questionEntityRepository.findById(questionId)
			.orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + questionId));
		checkQuestionBelongsToSet(questionSetId, question);

		question.updateQuestionStatus(QuestionStatusType.ACCESS_PERMISSION);
		QuestionStatusMessage message = new QuestionStatusMessage(
			questionSetId,
			questionId,
			QuestionStatusType.ACCESS_PERMISSION
		);

		questionWebSocketController.broadcastQuestionStatus(questionSetId, message);
	}

	/**
	 * 특정 문제의 풀이를 허용
	 */
	public void allowQuestionSolve(Long questionSetId, Long questionId) {
		QuestionEntity question = questionEntityRepository.findById(questionId)
			.orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + questionId));
		checkQuestionBelongsToSet(questionSetId, question);

		question.updateQuestionStatus(QuestionStatusType.SOLVE_PERMISSION);
		QuestionStatusMessage message = new QuestionStatusMessage(
			questionSetId,
			questionId,
			QuestionStatusType.SOLVE_PERMISSION
		);

		questionWebSocketController.broadcastQuestionStatus(questionSetId, message);
	}

	private void checkQuestionBelongsToSet(Long questionSetId, QuestionEntity question) {
		if (!question.getQuestionSet().getId().equals(questionSetId)) {
			throw new IllegalArgumentException(
				"Question with id " + question.getQuestionSet().getId() + " does not belong to Set with id "
					+ questionSetId
			);
		}
	}
}
