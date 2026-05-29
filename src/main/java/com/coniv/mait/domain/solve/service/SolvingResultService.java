package com.coniv.mait.domain.solve.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.question.service.component.QuestionSetReader;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.exception.QuestionSolveExceptionCode;
import com.coniv.mait.domain.solve.exception.QuestionSolvingException;
import com.coniv.mait.domain.solve.service.component.AnswerSubmitRecordReader;
import com.coniv.mait.domain.solve.service.dto.MySolveRecordDto;
import com.coniv.mait.domain.solve.service.dto.QuestionSolveResultDto;
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
			.map(questionId -> toQuestionResult(questionId, earliestByQuestionId.get(questionId)))
			.toList();

		int totalCount = results.size();
		int correctCount = (int)results.stream()
			.filter(QuestionSolveResultDto::isCorrect)
			.count();

		return MySolveRecordDto.of(questionSet.getId(), questionSet.getSolveMode(), totalCount, correctCount, results);
	}

	private QuestionSolveResultDto toQuestionResult(final Long questionId,
		final AnswerSubmitRecordEntity earliestRecord) {
		if (earliestRecord == null) {
			return QuestionSolveResultDto.unanswered(questionId);
		}
		return QuestionSolveResultDto.from(earliestRecord);
	}
}
