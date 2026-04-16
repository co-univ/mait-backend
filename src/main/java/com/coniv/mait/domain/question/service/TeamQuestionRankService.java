package com.coniv.mait.domain.question.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.question.service.component.QuestionSetReader;
import com.coniv.mait.domain.question.service.dto.PersonalAccuracyDto;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.entity.QuestionScorerEntity;
import com.coniv.mait.domain.solve.enums.SolvingStatus;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.solve.repository.QuestionScorerEntityRepository;
import com.coniv.mait.domain.solve.repository.SolvingSessionEntityRepository;
import com.coniv.mait.domain.solve.service.dto.RankDto;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.service.component.TeamReader;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.domain.user.service.component.UserReader;
import com.coniv.mait.domain.user.service.dto.UserDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamQuestionRankService {

	private final QuestionScorerEntityRepository questionScorerEntityRepository;
	private final AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;
	private final SolvingSessionEntityRepository solvingSessionEntityRepository;
	private final QuestionEntityRepository questionEntityRepository;

	private final QuestionReader questionReader;
	private final QuestionSetReader questionSetReader;
	private final TeamReader teamReader;
	private final UserReader userReader;
	private final TeamRoleValidator teamRoleValidator;

	public List<RankDto> getTeamQuestionScorerRank(final Long teamId) {
		TeamEntity team = teamReader.getTeam(teamId);

		List<QuestionEntity> completedQuestions = questionReader.getCompletedQuestionsInTeam(team);

		if (completedQuestions.isEmpty()) {
			return List.of();
		}

		List<UserEntity> teamUsers = userReader.getUsersByTeamFetchUser(team);
		if (teamUsers.isEmpty()) {
			return List.of();
		}
		List<Long> questionIds = completedQuestions.stream().map(QuestionEntity::getId).toList();

		Map<Long, Long> scorerCountByUserId = questionScorerEntityRepository.findAllByQuestionIdIn(questionIds).stream()
			.collect(Collectors.groupingBy(QuestionScorerEntity::getUserId, Collectors.counting()));

		List<RankDto> ranks = teamUsers.stream()
			.map(entry -> RankDto.builder()
				.user(UserDto.from(entry))
				.count(scorerCountByUserId.getOrDefault(entry.getId(), 0L))
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

		List<UserEntity> teamUsers = userReader.getUsersByTeamFetchUser(team);
		if (teamUsers.isEmpty()) {
			return List.of();
		}

		List<Long> questionIds = completedQuestions.stream().map(QuestionEntity::getId).toList();

		Map<Long, List<AnswerSubmitRecordEntity>> answerRecordsByUserId =
			answerSubmitRecordEntityRepository.findAllByQuestionIdIn(questionIds).stream()
				.collect(Collectors.groupingBy(AnswerSubmitRecordEntity::getUserId));

		Map<Long, Long> correctQuestionCountByUserId = answerRecordsByUserId.entrySet().stream()
			.collect(Collectors.toMap(
				Map.Entry::getKey,
				entry -> entry.getValue().stream()
					.collect(Collectors.groupingBy(AnswerSubmitRecordEntity::getQuestionId))
					.values().stream()
					.filter(records -> records.stream().allMatch(AnswerSubmitRecordEntity::isCorrect))
					.count()))
			.entrySet().stream()
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		List<RankDto> ranks = teamUsers.stream()
			.map(entry -> RankDto.builder()
				.user(UserDto.from(entry))
				.count(correctQuestionCountByUserId.getOrDefault(entry.getId(), 0L))
				.build())
			.sorted()
			.toList();

		calculateRanks(ranks);

		return ranks;
	}

	public PersonalAccuracyDto getPersonalAccuracy(final Long teamId, final Long userId) {
		teamRoleValidator.checkIsTeamMember(teamId, userId);

		TeamEntity team = teamReader.getTeam(teamId);

		List<Long> finishedLiveQuestionSetIds = questionSetReader.getFinishedLiveQuestionSetsInTeam(
			team.getId()).stream().map(QuestionSetEntity::getId).toList();

		List<Long> userCompletedQuestionSetIds = solvingSessionEntityRepository
			.findAllByUserIdAndStatusAndSolveModeAndQuestionSetTeamId(userId, SolvingStatus.COMPLETE,
				QuestionSetSolveMode.STUDY, teamId).stream()
			.map(session -> session.getQuestionSet().getId())
			.toList();

		List<Long> questionSetIds = Stream.concat(finishedLiveQuestionSetIds.stream(),
				userCompletedQuestionSetIds.stream())
			.toList();

		List<Long> questionIds = questionEntityRepository.findAllByQuestionSetIdIn(questionSetIds).stream()
			.map(QuestionEntity::getId)
			.toList();

		if (questionIds.isEmpty()) {
			return PersonalAccuracyDto.of(0, 0);
		}

		List<AnswerSubmitRecordEntity> userRecords =
			answerSubmitRecordEntityRepository.findAllByUserIdAndQuestionIdIn(userId, List.copyOf(questionIds));

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
