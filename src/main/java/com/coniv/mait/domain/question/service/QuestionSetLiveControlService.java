package com.coniv.mait.domain.question.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.dto.ParticipantDto;
import com.coniv.mait.domain.question.dto.QuestionSetActiveParticipantMessage;
import com.coniv.mait.domain.question.dto.QuestionSetStatusMessage;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.ParticipantStatus;
import com.coniv.mait.domain.question.enums.QuestionSetCommandType;
import com.coniv.mait.domain.question.enums.QuestionSetLiveStatus;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetParticipantRepository;
import com.coniv.mait.domain.question.service.component.QuestionWebSocketSender;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.global.exception.custom.ResourceNotBelongException;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionSetLiveControlService {

	private final QuestionSetEntityRepository questionSetEntityRepository;
	private final QuestionWebSocketSender questionWebSocketSender;
	private final TeamUserEntityRepository teamUserRepository;
	private final QuestionSetParticipantRepository questionSetParticipantRepository;

	@Transactional
	public void startLiveQuestionSet(Long questionSetId) {
		QuestionSetEntity questionSet = findQuestionSetById(questionSetId);
		questionSet.startLiveQuestionSet();

		questionSetParticipantRepository.deleteAllByQuestionSet(questionSet);
		questionSetParticipantRepository.flush();

		List<QuestionSetParticipantEntity> participants = findParticipantUsers(questionSet).stream()
			.map(user -> QuestionSetParticipantEntity.createActiveParticipant(questionSet, user))
			.toList();
		questionSetParticipantRepository.saveAll(participants);

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

	private List<UserEntity> findParticipantUsers(QuestionSetEntity questionSet) {
		return teamUserRepository.findAllByTeamId(questionSet.getTeamId()).stream()
			.map(TeamUserEntity::getUser)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<ParticipantDto> getActiveParticipants(Long questionSetId) {
		QuestionSetEntity questionSet = findQuestionSetById(questionSetId);

		return questionSetParticipantRepository.findAllByQuestionSetWithFetchJoinUser(questionSet).stream()
			.filter(participant -> participant.getStatus() == ParticipantStatus.ACTIVE)
			.map(ParticipantDto::from)
			.toList();
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

	private void sendActiveParticipantsUpdateMessage(Long questionSetId,
		List<QuestionSetParticipantEntity> activeParticipants) {
		List<ParticipantDto> participants = activeParticipants.stream()
			.map(ParticipantDto::from)
			.sorted(Comparator.comparing(ParticipantDto::getParticipantName))
			.toList();

		QuestionSetStatusMessage message = QuestionSetActiveParticipantMessage.builder()
			.questionSetId(questionSetId)
			.commandType(QuestionSetCommandType.ACTIVE_PARTICIPANTS)
			.activeParticipants(participants)
			.build();

		questionWebSocketSender.broadcastQuestionStatus(questionSetId, message);
		log.info("Updated active participants for question set ID: {}", questionSetId);
	}
}
