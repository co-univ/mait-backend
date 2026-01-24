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

	private final UserEntityRepository userEntityRepository;
	private final QuestionSetEntityRepository questionSetEntityRepository;
	private final QuestionEntityRepository questionEntityRepository;
	private final QuestionScorerEntityRepository questionScorerEntityRepository;
	private final AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	@Transactional(readOnly = true)
	public TeamQuestionRankCombinedDto getTeamQuestionRankCombined(Long teamId) {
		List<Long> questionIds = getCompletedQuestionIds(teamId);

		if (questionIds.isEmpty()) {
			return TeamQuestionRankCombinedDto.of(List.of(), List.of());
		}

		// Scorer와 정답 레코드 모두 조회
		List<QuestionScorerEntity> scorers = questionScorerEntityRepository.findAllByQuestionIdIn(questionIds);
		List<AnswerSubmitRecordEntity> correctAnswers =
			answerSubmitRecordEntityRepository.findAllByQuestionIdInAndIsCorrect(questionIds, true);

		// 랭킹 계산
		Map<Long, Long> scorerCountByUserId = scorers.stream()
			.collect(Collectors.groupingBy(
				QuestionScorerEntity::getUserId,
				Collectors.counting()
			));

		Map<Long, Long> correctCountByUserId = correctAnswers.stream()
			.collect(Collectors.groupingBy(
				AnswerSubmitRecordEntity::getUserId,
				Collectors.mapping(AnswerSubmitRecordEntity::getQuestionId, Collectors.collectingAndThen(
						Collectors.toSet(),
						set -> (long)set.size()
					)
				)
			));

		List<Long> topScorerUserIds = getTopUserIds(scorerCountByUserId);
		List<Long> topCorrectUserIds = getTopUserIds(correctCountByUserId);

		// 사용자 정보 조회
		Map<Long, UserEntity> userMap = getUserMap(topScorerUserIds, topCorrectUserIds);

		// DTO 생성
		List<TeamQuestionRankDto> scorerRank = createRankDtos(topScorerUserIds, userMap, scorerCountByUserId);
		List<TeamQuestionRankDto> correctAnswerRank = createRankDtos(topCorrectUserIds, userMap, correctCountByUserId);

		return TeamQuestionRankCombinedDto.of(scorerRank, correctAnswerRank);
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

	private List<Long> getTopUserIds(Map<Long, Long> countByUserId) {
		return countByUserId.entrySet().stream()
			.sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
			.limit(3)
			.map(Map.Entry::getKey)
			.toList();
	}

	private Map<Long, UserEntity> getUserMap(List<Long> topScorerUserIds, List<Long> topCorrectUserIds) {
		List<Long> allUserIds = new ArrayList<>(topScorerUserIds);
		allUserIds.addAll(topCorrectUserIds);
		List<Long> distinctUserIds = allUserIds.stream().distinct().toList();

		if (distinctUserIds.isEmpty()) {
			return Map.of();
		}

		return userEntityRepository.findAllById(distinctUserIds).stream()
			.collect(Collectors.toMap(UserEntity::getId, user -> user));
	}

	private List<TeamQuestionRankDto> createRankDtos(
		List<Long> userIds,
		Map<Long, UserEntity> userMap,
		Map<Long, Long> countByUserId
	) {
		return userIds.stream()
			.map(userId -> {
				UserEntity user = userMap.get(userId);
				Long count = countByUserId.get(userId);
				return TeamQuestionRankDto.of(user.getId(), user.getName(), user.getNickname(), count);
			})
			.toList();
	}
}
