package com.coniv.mait.domain.question.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.dto.AnswerRankDto;
import com.coniv.mait.domain.question.dto.ParticipantDto;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.question.enums.ParticipantStatus;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetParticipantRepository;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.question.service.dto.ParticipantCorrectAnswerRankDto;
import com.coniv.mait.domain.question.service.dto.ParticipantCorrectAnswersDto;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.entity.QuestionScorerEntity;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.solve.repository.QuestionScorerEntityRepository;
import com.coniv.mait.domain.team.service.component.TeamReader;
import com.coniv.mait.domain.user.service.component.UserReader;
import com.coniv.mait.domain.user.service.dto.UserDto;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionRankService {

	private final UserReader userReader;

	private final TeamReader teamReader;

	private final QuestionReader questionReader;

	private final QuestionSetParticipantRepository questionSetParticipantRepository;

	private final QuestionSetEntityRepository questionSetEntityRepository;

	private final QuestionEntityRepository questionEntityRepository;

	private final AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;
	private final QuestionScorerEntityRepository questionScorerEntityRepository;

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

	@Transactional(readOnly = true)
	public List<AnswerRankDto> getCorrectorsByQuestionSetId(final Long questionSetId) {
		QuestionSetEntity questionSet = findQuestionSetById(questionSetId);

		List<Long> questionIds = questionReader.getQuestionsByQuestionSet(questionSet).stream()
			.map(QuestionEntity::getId).toList();

		Map<Long, Long> answerCountByUserId = answerSubmitRecordEntityRepository.findAllByQuestionIdInAndIsCorrect(
				questionIds, true).stream()
			.collect(Collectors.groupingBy(AnswerSubmitRecordEntity::getUserId, Collectors.counting()));

		Map<Long, List<UserDto>> usersByCorrectCount =
			questionSetParticipantRepository.findAllByQuestionSetWithFetchJoinUser(questionSet).stream()
				.map(QuestionSetParticipantEntity::getUser)
				.map(UserDto::from)
				.collect(Collectors.groupingBy(
					user -> answerCountByUserId.getOrDefault(user.getId(), 0L)
				));

		return usersByCorrectCount.entrySet().stream()
			.map(entry -> AnswerRankDto.builder()
				.count(entry.getKey())
				.users(entry.getValue())
				.build())
			.sorted(Comparator.comparing(AnswerRankDto::getCount).reversed())
			.toList();
	}

	@Transactional(readOnly = true)
	public List<AnswerRankDto> getScorersByQuestionSetId(final Long questionSetId) {
		QuestionSetEntity questionSet = findQuestionSetById(questionSetId);

		List<Long> questionIds = questionReader.getQuestionsByQuestionSet(questionSet).stream()
			.map(QuestionEntity::getId).toList();

		Map<Long, Long> scoreCountByUserId = questionScorerEntityRepository.findAllByQuestionIdIn(questionIds).stream()
			.collect(Collectors.groupingBy(QuestionScorerEntity::getUserId, Collectors.counting()));

		Map<Long, List<UserDto>> usersByScoreCount =
			questionSetParticipantRepository.findAllByQuestionSetWithFetchJoinUser(questionSet).stream()
				.map(QuestionSetParticipantEntity::getUser)
				.map(UserDto::from)
				.collect(Collectors.groupingBy(
					user -> scoreCountByUserId.getOrDefault(user.getId(), 0L)
				));

		return usersByScoreCount.entrySet().stream()
			.map(entry -> AnswerRankDto.builder()
				.count(entry.getKey())
				.users(entry.getValue())
				.build())
			.sorted(Comparator.comparing(AnswerRankDto::getCount).reversed())
			.toList();
	}
}
