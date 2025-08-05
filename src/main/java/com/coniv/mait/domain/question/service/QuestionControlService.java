package com.coniv.mait.domain.question.service;

import org.springframework.stereotype.Service;

import com.coniv.mait.domain.question.dto.QuestionStatusMessage;
import com.coniv.mait.domain.question.enums.QuizStatusType;
import com.coniv.mait.web.question.controller.QuestionWebSocketController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionControlService {

	private final QuestionWebSocketController questionWebSocketController;

	/**
	 * 특정 문제의 접근을 허용
	 */
	public void allowQuestionAccess(Long quizSetId, Long questionId) {
		QuestionStatusMessage message = new QuestionStatusMessage(
			quizSetId,
			questionId,
			QuizStatusType.ACCESS_PERMISSION
		);

		questionWebSocketController.broadcastQuestionStatus(quizSetId, message);
	}

	/**
	 * 특정 문제의 풀이를 허용
	 */
	public void allowQuestionSolve(Long quizSetId, Long questionId) {
		QuestionStatusMessage message = new QuestionStatusMessage(
			quizSetId,
			questionId,
			QuizStatusType.SOLVE_PERMISSION
		);

		questionWebSocketController.broadcastQuestionStatus(quizSetId, message);
	}
}
