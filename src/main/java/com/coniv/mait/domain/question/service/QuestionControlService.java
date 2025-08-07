package com.coniv.mait.domain.question.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.dto.QuestionStatusMessage;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionStatusType;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.service.component.QuestionWebSocketSender;
import com.coniv.mait.global.exception.custom.QuestionSetLiveException;
import com.coniv.mait.global.exception.custom.ResourceNotBelongException;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionControlService {

	private final QuestionWebSocketSender questionWebSocketSender;
	private final QuestionEntityRepository questionEntityRepository;

	/**
	 * 특정 문제의 접근을 허용
	 */
	@Transactional
	public void allowQuestionAccess(Long questionSetId, Long questionId) {
		QuestionEntity question = questionEntityRepository.findById(questionId)
			.orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + questionId));
		checkQuestionBelongsToSet(questionSetId, question);
		// checkQuestionSetIsOnLive(question.getQuestionSet()); //TODO: 비즈니스 익셉션 추가 후 주석 해제

		closeAllQuestionStatus(questionSetId);

		question.updateQuestionStatus(QuestionStatusType.ACCESS_PERMISSION);
		QuestionStatusMessage message = QuestionStatusMessage.builder()
			.questionSetId(questionSetId)
			.questionId(questionId)
			.statusType(QuestionStatusType.ACCESS_PERMISSION)
			.build();

		questionWebSocketSender.broadcastQuestionStatus(questionSetId, message);
	}

	/**
	 * 특정 문제의 풀이를 허용
	 */
	@Transactional
	public void allowQuestionSolve(Long questionSetId, Long questionId) {
		QuestionEntity question = questionEntityRepository.findById(questionId)
			.orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + questionId));
		if (question.getQuestionStatus() != QuestionStatusType.ACCESS_PERMISSION) {
			throw new QuestionSetLiveException("Question must be in ACCESS_PERMISSION status before solving.");
		}
		checkQuestionBelongsToSet(questionSetId, question);
		// checkQuestionSetIsOnLive(question.getQuestionSet()); //TODO: 비즈니스 익셉션 추가 후 주석 해제

		closeAllQuestionStatus(questionSetId);

		question.updateQuestionStatus(QuestionStatusType.SOLVE_PERMISSION);
		QuestionStatusMessage message = QuestionStatusMessage.builder()
			.questionSetId(questionSetId)
			.questionId(questionId)
			.statusType(QuestionStatusType.SOLVE_PERMISSION)
			.build();

		questionWebSocketSender.broadcastQuestionStatus(questionSetId, message);
	}

	private void checkQuestionBelongsToSet(Long questionSetId, QuestionEntity question) {
		if (!question.getQuestionSet().getId().equals(questionSetId)) {
			throw new ResourceNotBelongException(
				"Question with id " + question.getQuestionSet().getId() + " does not belong to Set with id "
					+ questionSetId
			);
		}
	}

	private void closeAllQuestionStatus(Long questionSetId) {
		// 모든 문제의 상태를 초기화
		questionEntityRepository.findAllByQuestionSetId(questionSetId)
			.forEach(question -> question.updateQuestionStatus(QuestionStatusType.NOT_OPEN));
	}

	//TODO: 비즈니스 익셉션 추가 후 주석 해제
	/*private void checkQuestionSetIsOnLive(QuestionSetEntity questionSet) {
		if (!questionSet.isOnLive()) {
			throw new IllegalArgumentException("QuestionSet with id " + questionSet.getId() + " is not on live.");
		}
	}*/

	//TODO: 신청 관리자가 해당 팀의 관리자인지 확인하는 로직 추가 필요
}
