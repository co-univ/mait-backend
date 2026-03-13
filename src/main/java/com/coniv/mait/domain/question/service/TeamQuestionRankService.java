package com.coniv.mait.domain.question.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.question.service.dto.PersonalAccuracyDto;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.entity.QuestionScorerEntity;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.solve.repository.QuestionScorerEntityRepository;
import com.coniv.mait.domain.solve.service.dto.RankDto;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.service.component.TeamReader;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.service.component.UserReader;
import com.coniv.mait.domain.user.service.dto.UserDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamQuestionRankService {

	private final QuestionScorerEntityRepository questionScorerEntityRepository;
	private final AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	private final QuestionReader questionReader;
	private final TeamReader teamReader;
	private final UserReader userReader;

	public List<RankDto> getTeamQuestionScorerRank(final Long teamId) {
		TeamEntity team = teamReader.getTeam(teamId);

		List<QuestionEntity> completedQuestions = questionReader.getCompletedQuestionsInTeam(team);

		if (completedQuestions.isEmpty()) {
			return List.of();
		}
		List<Long> questionIds = completedQuestions.stream().map(QuestionEntity::getId).toList();

		Map<Long, Long> scorerCountByUserId = questionScorerEntityRepository.findAllByQuestionIdIn(questionIds).stream()
			.collect(Collectors.groupingBy(QuestionScorerEntity::getUserId, Collectors.counting()));

		Map<Long, UserEntity> userById = userReader.getUserById(scorerCountByUserId.keySet());

		List<RankDto> ranks = scorerCountByUserId.entrySet().stream()
			.map(entry -> RankDto.builder()
				.user(UserDto.from(userById.get(entry.getKey())))
				.count(entry.getValue())
				.build())
			.sorted()
			.toList();

		calculateRanks(ranks);

		return ranks;
	}

	public List<RankDto> getTeamQuestionCorrectAnswerRank(final Long teamId) {
		TeamEntity team = teamReader.getTeam(teamId);

		List<QuestionEntity> completedQuestions = questionReader.getCompletedQuestionsInTeam(team);

		if (completedQuestions.isEmpty()) {
			return List.of();
		}

		List<Long> questionIds = completedQuestions.stream().map(QuestionEntity::getId).toList();

		Map<Long, List<AnswerSubmitRecordEntity>> correctAnswersByUserId =
			answerSubmitRecordEntityRepository.findAllByQuestionIdInAndIsCorrect(questionIds, true).stream()
				.collect(Collectors.groupingBy(AnswerSubmitRecordEntity::getUserId));

		Map<Long, UserEntity> userById = userReader.getUserById(correctAnswersByUserId.keySet());

		List<RankDto> ranks = correctAnswersByUserId.entrySet().stream()
			.map(entry -> RankDto.builder()
				.user(UserDto.from(userById.get(entry.getKey())))
				.count(entry.getValue().size())
				.build())
			.sorted()
			.toList();

		calculateRanks(ranks);

		return ranks;
	}

	public PersonalAccuracyDto getPersonalAccuracy(final Long teamId, final Long userId) {
		TeamEntity team = teamReader.getTeam(teamId);

		List<QuestionEntity> completedQuestions = questionReader.getCompletedQuestionsInTeam(team);

		if (completedQuestions.isEmpty()) {
			return PersonalAccuracyDto.of(0, 0);
		}

		List<Long> questionIds = completedQuestions.stream().map(QuestionEntity::getId).toList();

		List<AnswerSubmitRecordEntity> userRecords =
			answerSubmitRecordEntityRepository.findAllByUserIdAndQuestionIdIn(userId, questionIds);

		Map<Long, List<AnswerSubmitRecordEntity>> recordsByQuestionId =
			userRecords.stream().collect(Collectors.groupingBy(AnswerSubmitRecordEntity::getQuestionId));

		long totalSolvedCount = recordsByQuestionId.size();

		long correctCount = recordsByQuestionId.values().stream()
			.filter(records -> records.stream().anyMatch(AnswerSubmitRecordEntity::isCorrect))
			.count();

		return PersonalAccuracyDto.of(totalSolvedCount, correctCount);
	}

	private void calculateRanks(List<RankDto> sortedRanks) {
		int rank = 1;
		for (int i = 0; i < sortedRanks.size(); i++) {
			if (i > 0 && sortedRanks.get(i).getCount() != sortedRanks.get(i - 1).getCount()) {
				rank++;
			}
			sortedRanks.get(i).setRank(rank);
		}
	}
}
