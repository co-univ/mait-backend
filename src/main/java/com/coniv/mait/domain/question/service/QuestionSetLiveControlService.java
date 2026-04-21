package com.coniv.mait.domain.question.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.dto.ParticipantDto;
import com.coniv.mait.domain.question.dto.QuestionSetParticipantsMessage;
import com.coniv.mait.domain.question.dto.QuestionSetStatusMessage;
import com.coniv.mait.domain.question.dto.QuestionStatusMessage;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.question.enums.ParticipantStatus;
import com.coniv.mait.domain.question.enums.QuestionSetCommandType;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.enums.QuestionStatusType;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetParticipantRepository;
import com.coniv.mait.domain.question.service.component.QuestionSetReader;
import com.coniv.mait.domain.question.service.component.QuestionWebSocketSender;
import com.coniv.mait.domain.question.service.dto.CurrentQuestionDto;
import com.coniv.mait.global.constant.WebSocketConstants;
import com.coniv.mait.web.question.dto.ParticipantSendType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionSetLiveControlService {

	private final QuestionSetEntityRepository questionSetEntityRepository;
	private final QuestionWebSocketSender questionWebSocketSender;
	private final QuestionSetParticipantRepository questionSetParticipantRepository;
	private final QuestionEntityRepository questionEntityRepository;
	private final SimpMessagingTemplate messagingTemplate;
	private final QuestionSetParticipantService questionSetParticipantService;
	private final QuestionService questionService;
	private final QuestionSetReader questionSetReader;

	@Transactional
	public void startLiveQuestionSet(Long questionSetId) {
		QuestionSetEntity questionSet = findQuestionSetById(questionSetId);
		questionSet.startLiveQuestionSet();

		QuestionSetStatusMessage message = QuestionSetStatusMessage.builder()
			.questionSetId(questionSetId)
			.commandType(QuestionSetCommandType.LIVE_START)
			.build();
		questionWebSocketSender.broadcastQuestionStatus(questionSetId, message);

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
		questionWebSocketSender.broadcastQuestionStatus(questionSetId, message);
		log.info("Ended live question set with ID: {}", questionSetId);
	}

	private QuestionSetEntity findQuestionSetById(Long questionSetId) {
		return questionSetReader.getActiveQuestionSet(questionSetId);
	}

	@Transactional(readOnly = true)
	public List<ParticipantDto> getActiveParticipants(Long questionSetId) {
		QuestionSetEntity questionSet = findQuestionSetById(questionSetId);

		return questionSetParticipantRepository.findAllByQuestionSetWithFetchJoinUser(questionSet).stream()
			.filter(participant -> participant.getStatus() == ParticipantStatus.ACTIVE)
			.map(ParticipantDto::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public void sendWinner(Long questionSetId, List<Long> winnerIds) {
		QuestionSetEntity questionSet = findQuestionSetById(questionSetId);

		List<QuestionSetParticipantEntity> participants =
			questionSetParticipantRepository.findAllByQuestionSetWithFetchJoinUser(questionSet);

		List<ParticipantDto> winners = participants.stream()
			.filter(participant -> winnerIds.contains(participant.getUser().getId()))
			.map(ParticipantDto::from)
			.sorted(Comparator.comparing(ParticipantDto::getParticipantName))
			.toList();

		QuestionSetStatusMessage message = QuestionSetParticipantsMessage.builder()
			.questionSetId(questionSetId)
			.commandType(QuestionSetCommandType.WINNER)
			.activeParticipants(winners)
			.build();

		questionWebSocketSender.broadcastQuestionStatus(questionSetId, message);
		log.info("Sent winners for question set ID: {}", questionSetId);
	}

	@Transactional
	public void updateActiveParticipants(Long questionSetId, List<Long> activeUserIds) {
		QuestionSetEntity questionSet = findQuestionSetById(questionSetId);

		List<QuestionSetParticipantEntity> allParticipants =
			questionSetParticipantRepository.findAllByQuestionSetWithFetchJoinUser(questionSet);
		Set<Long> activeIdSet = new HashSet<>(activeUserIds);

		allParticipants.forEach(p -> {
			ParticipantStatus newStatus = activeIdSet.contains(p.getUser().getId())
				? ParticipantStatus.ACTIVE
				: ParticipantStatus.ELIMINATED;
			p.updateStatus(newStatus);
		});

		// ACTIVE 상태인 참가자들만 필터링해서 메시지 전송
		List<QuestionSetParticipantEntity> activeParticipants = allParticipants.stream()
			.filter(participant -> participant.getStatus() == ParticipantStatus.ACTIVE)
			.toList();
		sendActiveParticipantsUpdateMessage(questionSetId, activeParticipants);
	}

	public void sendCurrentQuestionStatus(Long questionSetId, String sessionId) {
		QuestionSetEntity questionSet = findQuestionSetById(questionSetId);

		Optional<QuestionEntity> mayBeOpenQuestion = questionEntityRepository.findFirstByQuestionSetAndQuestionStatusIn(
			questionSet, List.of(QuestionStatusType.ACCESS_PERMISSION, QuestionStatusType.SOLVE_PERMISSION));

		String destination = WebSocketConstants.getQuestionSetParticipateTopic(questionSetId);

		QuestionStatusMessage message;
		if (mayBeOpenQuestion.isPresent()) {
			message = QuestionStatusMessage.builder()
				.questionSetId(questionSetId)
				.questionId(mayBeOpenQuestion.get().getId())
				.statusType(mayBeOpenQuestion.get().getQuestionStatus())
				.build();
		} else {
			message = QuestionStatusMessage.builder()
				.questionSetId(questionSetId)
				.statusType(QuestionStatusType.NOT_OPEN)
				.build();
		}
		log.info("Sending current question status for question set ID: {} sessionId = {}", questionSetId, sessionId);
		//messagingTemplate.convertAndSendToUser(sessionId, destination, message);
		questionWebSocketSender.broadcastQuestionStatus(questionSetId, message);
	}

	private void sendActiveParticipantsUpdateMessage(Long questionSetId,
		List<QuestionSetParticipantEntity> activeParticipants) {
		List<ParticipantDto> participants = activeParticipants.stream()
			.map(ParticipantDto::from)
			.sorted(Comparator.comparing(ParticipantDto::getParticipantName))
			.toList();

		QuestionSetStatusMessage message = QuestionSetParticipantsMessage.builder()
			.questionSetId(questionSetId)
			.commandType(QuestionSetCommandType.ACTIVE_PARTICIPANTS)
			.activeParticipants(participants)
			.build();

		questionWebSocketSender.broadcastQuestionStatus(questionSetId, message);
		log.info("Updated active participants for question set ID: {}", questionSetId);
	}

	public void handleParticipation(Long questionSetId, Long userId) {
		ParticipantDto participant = questionSetParticipantService.participateLiveQuestionSet(questionSetId, userId);
		CurrentQuestionDto currentQuestion = questionService.findCurrentQuestion(questionSetId);
		questionWebSocketSender.sendMyParticipationStatus(userId, questionSetId,
			participant.getStatus(), currentQuestion.getQuestionId(), currentQuestion.getQuestionStatus());
		log.info("[초기 상태 전송] userId={} questionSetId={} status={} questionId={} questionStatus={}",
			userId, questionSetId, participant.getStatus(),
			currentQuestion.getQuestionId(), currentQuestion.getQuestionStatus());
	}

	public void sendParticipants(Long questionSetId, ParticipantSendType type) {
		QuestionSetEntity questionSet = findQuestionSetById(questionSetId);

		List<ParticipantDto> activeParticipants =
			questionSetParticipantRepository.findAllByQuestionSetWithFetchJoinUser(questionSet).stream()
				.filter(QuestionSetParticipantEntity::isActive)
				.map(ParticipantDto::from)
				.toList();

		if (type == ParticipantSendType.NEXT_ROUND) {
			QuestionSetStatusMessage message = QuestionSetParticipantsMessage.builder()
				.questionSetId(questionSetId)
				.commandType(QuestionSetCommandType.ACTIVE_PARTICIPANTS)
				.activeParticipants(activeParticipants)
				.build();
			questionWebSocketSender.broadcastQuestionStatus(questionSetId, message);
			return;
		}

		List<ParticipantDto> winnerParticipants = activeParticipants.stream()
			.filter(ParticipantDto::isWinner)
			.toList();
		QuestionSetStatusMessage message = QuestionSetParticipantsMessage.builder()
			.questionSetId(questionSetId)
			.commandType(QuestionSetCommandType.WINNER)
			.activeParticipants(winnerParticipants)
			.build();
		questionWebSocketSender.broadcastQuestionStatus(questionSetId, message);
	}
}
