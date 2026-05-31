package com.coniv.mait.domain.statistic.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.question.service.component.QuestionSetReader;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.exception.QuestionSolveExceptionCode;
import com.coniv.mait.domain.solve.exception.QuestionSolvingException;
import com.coniv.mait.domain.solve.service.component.AnswerSubmitRecordReader;
import com.coniv.mait.domain.solve.service.component.QuestionParticipantReader;
import com.coniv.mait.domain.solve.service.dto.QuestionSolveResultDto;
import com.coniv.mait.domain.statistic.service.component.QuestionSetStatisticCalculator;
import com.coniv.mait.domain.statistic.service.dto.MySolveRecordDto;
import com.coniv.mait.domain.statistic.service.dto.QuestionSetStatisticDto;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.global.auth.model.MaitUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SolvingResultService {

	private final QuestionSetReader questionSetReader;
	private final QuestionReader questionReader;
	private final TeamRoleValidator teamRoleValidator;
	private final AnswerSubmitRecordReader answerSubmitRecordReader;
	private final QuestionParticipantReader questionParticipantReader;
	private final QuestionEntityRepository questionEntityRepository;
	private final QuestionSetStatisticCalculator questionSetStatisticCalculator;

	@Transactional(readOnly = true)
	public MySolveRecordDto getSolvingResults(final MaitUser user, final Long questionSetId) {
		QuestionSetEntity questionSet = questionSetReader.getQuestionSet(questionSetId);

		teamRoleValidator.checkHasSolveQuestionAuthorityInTeam(questionSet.getTeamId(), user.id());

		List<Long> orderedQuestionIds = questionReader.getOrderedQuestions(questionSetId)
			.stream().map(QuestionEntity::getId).toList();

		Map<Long, AnswerSubmitRecordEntity> earliestByQuestionId =
			answerSubmitRecordReader.getEarliestByQuestionId(user.id(), orderedQuestionIds);

		if (earliestByQuestionId.isEmpty()) {
			throw new QuestionSolvingException(QuestionSolveExceptionCode.NO_SOLVE_RECORD);
		}

		List<QuestionSolveResultDto> results = orderedQuestionIds.stream()
			.map(questionId -> QuestionSolveResultDto.of(questionId, earliestByQuestionId.get(questionId)))
			.toList();

		int totalCount = results.size();
		int correctCount = (int)results.stream()
			.filter(QuestionSolveResultDto::isCorrect)
			.count();

		return MySolveRecordDto.of(questionSet.getId(), questionSet.getSolveMode(), totalCount, correctCount, results);
	}

	@Transactional(readOnly = true)
	public List<QuestionSetStatisticDto> getTeamQuestionSetStatistics(final MaitUser maitUser, final Long teamId) {
		teamRoleValidator.checkHasSolveQuestionAuthorityInTeam(teamId, maitUser.id());

		List<QuestionSetEntity> liveQuestionSets = questionSetReader.getFinishedLiveQuestionSetsBySolveModeInTeam(
			teamId, QuestionSetSolveMode.LIVE_TIME);
		List<QuestionSetEntity> studyModeQuestionSets = questionSetReader.getFinishedLiveQuestionSetsBySolveModeInTeam(
			teamId, QuestionSetSolveMode.STUDY);

		List<QuestionSetEntity> questionSets = Stream.concat(liveQuestionSets.stream(), studyModeQuestionSets.stream())
			.toList();
		if (questionSets.isEmpty()) {
			return List.of();
		}

		List<Long> questionSetIds = questionSets.stream().map(QuestionSetEntity::getId).toList();
		Map<Long, List<QuestionEntity>> questionsByQuestionSetId =
			questionEntityRepository.findAllByQuestionSetIdIn(questionSetIds).stream()
				.collect(Collectors.groupingBy(question -> question.getQuestionSet().getId()));

		Map<Long, List<QuestionSetParticipantEntity>> winnersByQuestionSetId = questionParticipantReader
			.getWinnersByQuestionSetId(liveQuestionSets.stream().map(QuestionSetEntity::getId).toList());

		Map<Long, Double> myCorrectRatesByQuestionSetId =
			questionSetStatisticCalculator.calculateUserCorrectRates(maitUser.id(), questionsByQuestionSetId);
		Map<Long, Double> averageCorrectRatesByQuestionSetId =
			questionSetStatisticCalculator.calculateOverallCorrectRates(questionsByQuestionSetId);

		return questionSets.stream()
			.map(questionSet -> QuestionSetStatisticDto.of(questionSet,
				winnersByQuestionSetId.getOrDefault(questionSet.getId(), List.of()),
				myCorrectRatesByQuestionSetId.get(questionSet.getId()),
				averageCorrectRatesByQuestionSetId.getOrDefault(questionSet.getId(), 0.0)))
			.sorted(Comparator.comparing(QuestionSetStatisticDto::getSolvedAt,
				Comparator.nullsLast(Comparator.reverseOrder())))
			.toList();
	}
}
