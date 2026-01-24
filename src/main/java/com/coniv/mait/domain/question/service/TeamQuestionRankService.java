package com.coniv.mait.domain.question.service;

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
import com.coniv.mait.domain.solve.entity.QuestionScorerEntity;
import com.coniv.mait.domain.solve.repository.QuestionScorerEntityRepository;
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

	@Transactional(readOnly = true)
	public List<TeamQuestionRankDto> getTeamQuestionRank(Long teamId) {
		// 1. teamId에서 진행한 QuestionSetOngoingStatus가 AFTER인 QuestionSet 가져오기
		List<QuestionSetEntity> completedQuestionSets = questionSetEntityRepository.findAllByTeamId(teamId)
			.stream()
			.filter(qs -> qs.getOngoingStatus() == QuestionSetOngoingStatus.AFTER)
			.toList();

		if (completedQuestionSets.isEmpty()) {
			return List.of();
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

		// 3. 해당 퀴즈의 questionScorer 가져오기
		List<QuestionScorerEntity> scorers = questionScorerEntityRepository.findAllByQuestionIdIn(questionIds);

		// 4. userId별로 scorer 갯수를 집계하고 가장 많은 3명 추출
		Map<Long, Long> scorerCountByUserId = scorers.stream()
			.collect(Collectors.groupingBy(
				QuestionScorerEntity::getUserId,
				Collectors.counting()
			));

		//TODO: 동점자 처리 로직 필요할 수도(다 같은 점수일 때 모두 포함 등)

		List<Long> topUserIds = scorerCountByUserId.entrySet().stream()
			.sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
			.limit(3)
			.map(Map.Entry::getKey)
			.toList();

		if (topUserIds.isEmpty()) {
			return List.of();
		}

		// 5. 사용자 정보를 가져와서 DTO 생성
		List<UserEntity> topUsers = userEntityRepository.findAllById(topUserIds);
		Map<Long, UserEntity> userMap = topUsers.stream()
			.collect(Collectors.toMap(UserEntity::getId, user -> user));

		return topUserIds.stream()
			.map(userId -> {
				UserEntity user = userMap.get(userId);
				Long count = scorerCountByUserId.get(userId);
				return TeamQuestionRankDto.of(user.getId(), user.getName(), user.getNickname(), count);
			})
			.toList();
	}
}
