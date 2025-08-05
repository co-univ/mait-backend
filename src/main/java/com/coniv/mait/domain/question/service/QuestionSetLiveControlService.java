package com.coniv.mait.domain.question.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.dto.QuestionSetStatusMessage;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetCommandType;
import com.coniv.mait.domain.question.enums.QuestionSetLiveStatus;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.global.exception.custom.ResourceNotBelongException;
import com.coniv.mait.web.question.controller.QuestionWebSocketController;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionSetLiveControlService {

	private final QuestionSetEntityRepository questionSetEntityRepository;
	private final QuestionWebSocketController questionWebSocketController;

	@Transactional
	public void startLiveQuestionSet(Long questionSetId) {
		QuestionSetEntity questionSet = findQuestionSetById(questionSetId);
		questionSet.startLiveQuestionSet();

		QuestionSetStatusMessage message = QuestionSetStatusMessage.builder()
			.questionSetId(questionSetId)
			.commandType(QuestionSetCommandType.LIVE_START)
			.build();
		questionWebSocketController.broadcastQuestionStatus(questionSetId, message);

		log.info("Started live question set with ID: {}", questionSetId);
	}

	@Transactional
	public void endLiveQuestionSet(Long questionSetId) {
		QuestionSetEntity questionSet = findQuestionSetById(questionSetId);
		questionSet.endLiveQuestionSet();

		QuestionSetStatusMessage message = QuestionSetStatusMessage.builder()
			.questionSetId(questionSetId)
			.commandType(QuestionSetCommandType.LIVE_END)
			.build();
		questionWebSocketController.broadcastQuestionStatus(questionSetId, message);
		log.info("Ended live question set with ID: {}", questionSetId);
	}

	@Transactional(readOnly = true)
	public QuestionSetLiveStatus getLiveStatus(Long questionSetId) {
		QuestionSetEntity questionSet = findQuestionSetById(questionSetId);
		if (questionSet.getDeliveryMode() == DeliveryMode.REVIEW) {
			throw new ResourceNotBelongException("review mode can't find live status.");
		}
		return questionSet.getLiveStatus();
	}

	private QuestionSetEntity findQuestionSetById(Long questionSetId) {
		return questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("QuestionSet not found with id: " + questionSetId));
	}
}
