package com.coniv.mait.domain.question.service.component;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.coniv.mait.domain.question.dto.ParticipantDto;
import com.coniv.mait.domain.question.dto.QuestionSetParticipationStatusMessage;
import com.coniv.mait.domain.question.dto.QuestionSetStatusMessage;
import com.coniv.mait.domain.question.dto.QuestionStatusMessage;
import com.coniv.mait.domain.question.enums.ParticipantStatus;
import com.coniv.mait.domain.question.enums.QuestionStatusType;
import com.coniv.mait.global.constant.WebSocketConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionWebSocketSender {

	private final SimpMessagingTemplate messagingTemplate;

	//관리자가 특정 퀴즈셋의 상태를 변경할 때 사용
	public void broadcastQuestionStatus(Long questionSetId, QuestionStatusMessage message) {
		String destination = WebSocketConstants.getQuestionSetParticipateTopic(questionSetId);
		messagingTemplate.convertAndSend(destination, message);

		log.info("Broadcasting question status to {}: {}", destination, message);
	}

	public void broadcastQuestionStatus(Long questionSetId, QuestionSetStatusMessage message) {
		String destination = WebSocketConstants.getQuestionSetParticipateTopic(questionSetId);
		messagingTemplate.convertAndSend(destination, message);

		log.info("Broadcasting question set status to {}: {}", destination, message);
	}

	public void broadcastNewParticipantToMaker(Long questionSetId, ParticipantDto participant) {
		String destination = WebSocketConstants.getQuestionSetManageTopic(questionSetId);
		messagingTemplate.convertAndSend(destination, participant);

		log.info("Broadcasting new participant to maker {}: userId={}", destination, participant.getUserId());
	}

	public void sendParticipantStatusChange(Long userId, Long questionSetId, ParticipantStatus participantStatus) {
		String destination = WebSocketConstants.getQuestionSetParticipationStatusQueue(questionSetId);
		QuestionSetParticipationStatusMessage message = QuestionSetParticipationStatusMessage.builder()
			.questionSetId(questionSetId)
			.participantStatus(participantStatus)
			.build();

		messagingTemplate.convertAndSendToUser(String.valueOf(userId), destination, message);

		log.info("Sending status change to userId={} destination={} participantStatus={}",
			userId, destination, participantStatus);
	}

	public void sendMyParticipationStatus(Long userId, Long questionSetId, ParticipantStatus participantStatus,
		Long questionId, QuestionStatusType statusType) {
		String destination = WebSocketConstants.getQuestionSetParticipationStatusQueue(questionSetId);
		QuestionSetParticipationStatusMessage message = QuestionSetParticipationStatusMessage.builder()
			.questionSetId(questionSetId)
			.participantStatus(participantStatus)
			.questionId(questionId)
			.statusType(statusType)
			.build();

		messagingTemplate.convertAndSendToUser(String.valueOf(userId), destination, message);

		log.info(
			"Sending participation status to userId={} destination={} participantStatus={} questionId={} statusType={}",
			userId, destination, participantStatus, questionId, statusType);
	}
}
