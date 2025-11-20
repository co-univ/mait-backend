package com.coniv.mait.domain.question.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.dto.ParticipantDto;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.question.enums.ParticipantStatus;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetParticipantRepository;
import com.coniv.mait.global.exception.custom.UserParameterException;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionSetParticipantService {

	private final QuestionSetEntityRepository questionSetEntityRepository;

	private final QuestionSetParticipantRepository questionSetParticipantRepository;

	public List<ParticipantDto> getParticipants(final Long questionSetId) {
		QuestionSetEntity questionSet = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("해당 문제 셋을 조회할 수 없음 id: " + questionSetId));

		return questionSetParticipantRepository.findAllByQuestionSetWithFetchJoinUser(questionSet).stream()
			.map(ParticipantDto::from)
			.toList();

	}

	@Transactional
	public List<ParticipantDto> updateParticipantsStatus(final Long questionSetId,
		final List<ParticipantDto> activeUsers, final List<ParticipantDto> eliminatedUsers) {
		validateParticipants(activeUsers, eliminatedUsers);

		QuestionSetEntity questionSet = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("해당 문제 셋을 조회할 수 없음 id: " + questionSetId));

		Map<Long, QuestionSetParticipantEntity> participantByUserId = questionSetParticipantRepository
			.findAllByQuestionSetWithFetchJoinUser(
				questionSet)
			.stream()
			.collect(Collectors.toUnmodifiableMap(participant -> participant.getUser().getId(),
				Function.identity()));

		for (ParticipantDto activeUserId : activeUsers) {
			QuestionSetParticipantEntity activeUser = participantByUserId.get(activeUserId.getUserId());
			activeUser.updateStatus(ParticipantStatus.ACTIVE);
		}

		for (ParticipantDto eliminatedUser : eliminatedUsers) {
			QuestionSetParticipantEntity eliminated = participantByUserId.get(eliminatedUser.getUserId());
			eliminated.updateStatus(ParticipantStatus.ELIMINATED);
		}

		return participantByUserId.values().stream().map(ParticipantDto::from).toList();
	}

	private void validateParticipants(final List<ParticipantDto> activeDtos,
		final List<ParticipantDto> eliminatedDtos) {
		Set<Long> actives = activeDtos.stream().map(ParticipantDto::getUserId).collect(Collectors.toSet());
		Set<Long> eliminates = eliminatedDtos.stream().map(ParticipantDto::getUserId).collect(Collectors.toSet());

		if (!Collections.disjoint(actives, eliminates)) {
			throw new UserParameterException("제거 또는 추가할 셋에 중복된 유저가 존재");
		}
	}
}
