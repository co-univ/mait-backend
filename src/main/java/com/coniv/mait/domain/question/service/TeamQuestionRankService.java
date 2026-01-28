package com.coniv.mait.domain.question.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetOngoingStatus;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.entity.QuestionScorerEntity;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.solve.repository.QuestionScorerEntityRepository;
import com.coniv.mait.domain.team.service.dto.TeamQuestionRankCombinedDto;
import com.coniv.mait.domain.team.service.dto.TeamQuestionRankDto;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamQuestionRankService {

	private static final int RANK_LIMIT = 5;

	private final UserEntityRepository userEntityRepository;
	private final QuestionSetEntityRepository questionSetEntityRepository;
	private final QuestionEntityRepository questionEntityRepository;
	private final QuestionScorerEntityRepository questionScorerEntityRepository;
	private final AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	@Transactional(readOnly = true)
	public TeamQuestionRankCombinedDto getTeamQuestionScorerRank(Long teamId, Long currentUserId) {
		List<Long> questionIds = getCompletedQuestionIds(teamId);

		if (questionIds.isEmpty()) {
			return TeamQuestionRankCombinedDto.of(List.of(), null);
		}

		// Scorer 조회
		List<QuestionScorerEntity> scorers = questionScorerEntityRepository.findAllByQuestionIdIn(questionIds);

		// 랭킹 계산
		Map<Long, Long> scorerCountByUserId = calculateScorerCount(scorers);

		// 전체 사용자를 점수 순으로 정렬 (등수 계산용)
		List<Map.Entry<Long, Long>> sortedEntries = scorerCountByUserId.entrySet().stream()
			.sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
			.toList();

		// 등수 맵 생성
		Map<Long, Integer> rankByUserId = calculateRanks(sortedEntries);

		List<Long> topUserIds = getTopUserIds(scorerCountByUserId);

		// 사용자 정보 조회
		List<Long> allUserIds = new ArrayList<>(topUserIds);
		if (!allUserIds.contains(currentUserId)) {
			allUserIds.add(currentUserId);
		}
		Map<Long, UserEntity> userMap = getUserMap(allUserIds);

		// DTO 생성
		List<TeamQuestionRankDto> teamRank = createRankDto(topUserIds, userMap, scorerCountByUserId, rankByUserId);
		TeamQuestionRankDto myRank = createMyRankDto(currentUserId, userMap, scorerCountByUserId, rankByUserId);

		return TeamQuestionRankCombinedDto.of(teamRank, myRank);
	}

	@Transactional(readOnly = true)
	public TeamQuestionRankCombinedDto getTeamQuestionCorrectAnswerRank(Long teamId, Long currentUserId) {
		List<Long> questionIds = getCompletedQuestionIds(teamId);

		if (questionIds.isEmpty()) {
			return TeamQuestionRankCombinedDto.of(List.of(), null);
		}

		// 정답 레코드 조회
		List<AnswerSubmitRecordEntity> correctAnswers =
			answerSubmitRecordEntityRepository.findAllByQuestionIdInAndIsCorrect(questionIds, true);

		// 랭킹 계산
		Map<Long, Long> correctCountByUserId = calculateCorrectAnswerCount(correctAnswers);

		// 전체 사용자를 점수 순으로 정렬 (등수 계산용)
		List<Map.Entry<Long, Long>> sortedEntries = correctCountByUserId.entrySet().stream()
			.sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
			.toList();

		// 등수 맵 생성
		Map<Long, Integer> rankByUserId = calculateRanks(sortedEntries);

		List<Long> topUserIds = getTopUserIds(correctCountByUserId);

		// 사용자 정보 조회
		List<Long> allUserIds = new ArrayList<>(topUserIds);
		if (!allUserIds.contains(currentUserId)) {
			allUserIds.add(currentUserId);
		}
		Map<Long, UserEntity> userMap = getUserMap(allUserIds);

		// DTO 생성
		List<TeamQuestionRankDto> teamRank = createRankDto(topUserIds, userMap, correctCountByUserId, rankByUserId);
		TeamQuestionRankDto myRank = createMyRankDto(currentUserId, userMap, correctCountByUserId, rankByUserId);

		return TeamQuestionRankCombinedDto.of(teamRank, myRank);
	}

	private List<Long> getCompletedQuestionIds(Long teamId) {
		List<QuestionSetEntity> completedQuestionSets = questionSetEntityRepository.findAllByTeamId(teamId)
			.stream()
			.filter(qs -> qs.getOngoingStatus() == QuestionSetOngoingStatus.AFTER)
			.toList();

		if (completedQuestionSets.isEmpty()) {
			return List.of();
		}

		List<Long> questionSetIds = completedQuestionSets.stream()
			.map(QuestionSetEntity::getId)
			.toList();

		return questionSetIds.stream()
			.flatMap(qsId -> questionEntityRepository.findAllByQuestionSetId(qsId).stream())
			.map(QuestionEntity::getId)
			.toList();
	}

	private Map<Long, Long> calculateScorerCount(List<QuestionScorerEntity> scorers) {
		return scorers.stream()
			.collect(Collectors.groupingBy(
				QuestionScorerEntity::getUserId,
				Collectors.counting()
			));
	}

	private Map<Long, Long> calculateCorrectAnswerCount(List<AnswerSubmitRecordEntity> correctAnswers) {
		return correctAnswers.stream()
			.collect(Collectors.groupingBy(
				AnswerSubmitRecordEntity::getUserId,
				Collectors.mapping(
					AnswerSubmitRecordEntity::getQuestionId,
					Collectors.collectingAndThen(
						Collectors.toSet(),
						set -> (long)set.size()
					)
				)
			));
	}

	private List<Long> getTopUserIds(Map<Long, Long> countByUserId) {
		return countByUserId.entrySet().stream()
			.sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
			.limit(RANK_LIMIT)
			.map(Map.Entry::getKey)
			.toList();
	}

	private Map<Long, UserEntity> getUserMap(List<Long> userIds) {
		if (userIds.isEmpty()) {
			return Map.of();
		}

		return userEntityRepository.findAllById(userIds).stream()
			.collect(Collectors.toMap(UserEntity::getId, user -> user));
	}

	private Map<Long, Integer> calculateRanks(List<Map.Entry<Long, Long>> sortedEntries) {
		Map<Long, Integer> rankByUserId = new java.util.HashMap<>();
		int rank = 1;
		Long previousCount = null;
		int sameRankCount = 0;

		for (Map.Entry<Long, Long> entry : sortedEntries) {
			Long currentCount = entry.getValue();

			if (previousCount != null && !previousCount.equals(currentCount)) {
				rank += sameRankCount;
				sameRankCount = 1;
			} else {
				sameRankCount++;
			}

			rankByUserId.put(entry.getKey(), rank);
			previousCount = currentCount;
		}

		return rankByUserId;
	}

	private List<TeamQuestionRankDto> createRankDto(
		List<Long> userIds,
		Map<Long, UserEntity> userMap,
		Map<Long, Long> countByUserId,
		Map<Long, Integer> rankByUserId
	) {
		return userIds.stream()
			.map(userId -> {
				UserEntity user = userMap.get(userId);
				Long count = countByUserId.get(userId);
				Integer rank = rankByUserId.get(userId);
				return TeamQuestionRankDto.of(user.getId(), user.getName(), user.getNickname(), count, rank);
			})
			.toList();
	}

	private TeamQuestionRankDto createMyRankDto(
		Long userId,
		Map<Long, UserEntity> userMap,
		Map<Long, Long> countByUserId,
		Map<Long, Integer> rankByUserId
	) {
		if (!countByUserId.containsKey(userId)) {
			return null;
		}

		UserEntity user = userMap.get(userId);
		Long count = countByUserId.get(userId);
		Integer rank = rankByUserId.get(userId);
		return TeamQuestionRankDto.of(user.getId(), user.getName(), user.getNickname(), count, rank);
	}
}
