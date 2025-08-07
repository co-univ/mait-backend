package com.coniv.mait.domain.question.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.coniv.mait.domain.question.dto.ParticipantDto;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.question.enums.ParticipantStatus;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetParticipantRepository;
import com.coniv.mait.domain.question.service.dto.ParticipantCorrectAnswerRankDto;
import com.coniv.mait.domain.question.service.dto.ParticipantCorrectAnswersDto;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionRankService {

	private final QuestionSetParticipantRepository questionSetParticipantRepository;

	private final QuestionSetEntityRepository questionSetEntityRepository;

	private final QuestionEntityRepository questionEntityRepository;

	private final AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	public ParticipantCorrectAnswerRankDto getParticipantCorrectRank(Long questionSetId) {
		QuestionSetEntity questionSet = findQuestionSetById(questionSetId);
		List<QuestionSetParticipantEntity> allParticipants =
			questionSetParticipantRepository.findAllByQuestionSetWithFetchJoinUser(questionSet);

		List<Long> questionIds = questionEntityRepository.findAllByQuestionSetId(questionSetId).stream()
			.map(QuestionEntity::getId)
			.toList();

		List<AnswerSubmitRecordEntity> allCorrectRecords =
			answerSubmitRecordEntityRepository.findAllByQuestionIdInAndIsCorrect(questionIds, true);

		Map<Long, List<AnswerSubmitRecordEntity>> correctAnswersByUserId = allCorrectRecords.stream()
			.collect(Collectors.groupingBy(AnswerSubmitRecordEntity::getUserId));

		List<ParticipantCorrectAnswersDto> activeParticipants = new ArrayList<>();
		List<ParticipantCorrectAnswersDto> eliminateParticipants = new ArrayList<>();

		for (QuestionSetParticipantEntity participant : allParticipants) {
			ParticipantDto participantDto = ParticipantDto.from(participant);
			Long userId = participantDto.getUserId();

			// 해당 사용자가 맞춘 문제들
			List<AnswerSubmitRecordEntity> userCorrectAnswers =
				correctAnswersByUserId.getOrDefault(userId, List.of());

			// 맞춘 문제 수 계산
			long correctCount = userCorrectAnswers.stream()
				.map(AnswerSubmitRecordEntity::getQuestionId)
				.distinct()
				.count();

			ParticipantCorrectAnswersDto participantResult = ParticipantCorrectAnswersDto.from(participantDto,
				correctCount);

			// 참가자 상태에 따라 분류
			if (participant.getStatus() == ParticipantStatus.ACTIVE) {
				activeParticipants.add(participantResult);
			} else {
				eliminateParticipants.add(participantResult);
			}
		}

		// 맞춘 문제 수 기준으로 내림차순 정렬 (reverse sort)
		activeParticipants.sort(Comparator.comparing(ParticipantCorrectAnswersDto::getCorrectAnswerCount).reversed());
		eliminateParticipants.sort(
			Comparator.comparing(ParticipantCorrectAnswersDto::getCorrectAnswerCount).reversed());

		return ParticipantCorrectAnswerRankDto.of(activeParticipants, eliminateParticipants);
	}

	private QuestionSetEntity findQuestionSetById(Long questionSetId) {
		return questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("해당 문제 세트가 존재하지 않습니다."));
	}
}
