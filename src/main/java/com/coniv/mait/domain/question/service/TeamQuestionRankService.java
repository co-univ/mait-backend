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
		// 1. teamId에서 진행한 QuestionSetOngoingStatus가 AFTER인 QuestionSet 가져오기
		List<QuestionSetEntity> completedQuestionSets = questionSetEntityRepository.findAllByTeamId(teamId)
			.stream()
			.filter(qs -> qs.getOngoingStatus() == QuestionSetOngoingStatus.AFTER)
			.toList();

		if (completedQuestionSets.isEmpty()) {
			return TeamQuestionRankCombinedDto.of(List.of(), List.of());
		}

		// 2. 해당 questionSet에 포함된 Question 가져오기
		List<Long> questionSetIds = completedQuestionSets.stream()
			.map(QuestionSetEntity::getId)
			.toList();

		List<QuestionEntity> questions = questionSetIds.stream()
			.flatMap(qsId -> questionEntityRepository.findAllByQuestionSetId(qsId).stream())
			.toList();

		List<Long> questionIds = questions.stream()
			.map(QuestionEntity::getId)
			.toList();

		// 3. Scorer와 정답 레코드 모두 조회
		List<QuestionScorerEntity> scorers = questionScorerEntityRepository.findAllByQuestionIdIn(questionIds);
		List<AnswerSubmitRecordEntity> correctAnswers =
			answerSubmitRecordEntityRepository.findAllByQuestionIdInAndIsCorrect(questionIds, true);

		// 4-1. Scorer 랭킹 계산
		Map<Long, Long> scorerCountByUserId = scorers.stream()
			.collect(Collectors.groupingBy(
				QuestionScorerEntity::getUserId,
				Collectors.counting()
			));

		List<Long> topScorerUserIds = scorerCountByUserId.entrySet().stream()
			.sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
			.limit(3)
			.map(Map.Entry::getKey)
			.toList();

		// 4-2. 정답자 랭킹 계산
		Map<Long, Long> correctCountByUserId = correctAnswers.stream()
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

		List<Long> topCorrectUserIds = correctCountByUserId.entrySet().stream()
			.sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
			.limit(3)
			.map(Map.Entry::getKey)
			.toList();

		// 5. 모든 관련 사용자 정보 한 번에 조회
		List<Long> allUserIds = new ArrayList<>(topScorerUserIds);
		allUserIds.addAll(topCorrectUserIds);
		List<Long> distinctUserIds = allUserIds.stream().distinct().toList();

		List<UserEntity> users = distinctUserIds.isEmpty() ? List.of() :
			userEntityRepository.findAllById(distinctUserIds);
		Map<Long, UserEntity> userMap = users.stream()
			.collect(Collectors.toMap(UserEntity::getId, user -> user));

		// 6. DTO 생성
		List<TeamQuestionRankDto> scorerRank = topScorerUserIds.stream()
			.map(userId -> {
				UserEntity user = userMap.get(userId);
				Long count = scorerCountByUserId.get(userId);
				return TeamQuestionRankDto.of(user.getId(), user.getName(), user.getNickname(), count);
			})
			.toList();

		List<TeamQuestionRankDto> correctAnswerRank = topCorrectUserIds.stream()
			.map(userId -> {
				UserEntity user = userMap.get(userId);
				Long count = correctCountByUserId.get(userId);
				return TeamQuestionRankDto.of(user.getId(), user.getName(), user.getNickname(), count);
			})
			.toList();

		return TeamQuestionRankCombinedDto.of(scorerRank, correctAnswerRank);
	}
}
