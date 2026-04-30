package com.coniv.mait.domain.question.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.service.component.QuestionSetReader;
import com.coniv.mait.domain.solve.enums.SolvingStatus;
import com.coniv.mait.domain.solve.repository.SolvingSessionEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.global.auth.model.MaitUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionSetStudyControlService {

	private final QuestionSetReader questionSetReader;
	private final TeamRoleValidator teamRoleValidator;
	private final SolvingSessionEntityRepository solvingSessionEntityRepository;
	private final TeamUserEntityRepository teamUserEntityRepository;

	@Transactional
	public void startStudyQuestionSet(final MaitUser user, final Long questionSetId) {
		QuestionSetEntity questionSet = questionSetReader.getQuestionSet(questionSetId);
		teamRoleValidator.checkHasCreateQuestionSetAuthority(questionSet.getTeamId(), user.id());
		questionSet.startStudyQuestionSet();
		log.info("[학습 문제 셋 시작] questionSetId={}, teamId={}, startedBy={}",
			questionSetId, questionSet.getTeamId(), user.id());
	}

	@Transactional
	public void endStudyQuestionSet(final MaitUser user, final Long questionSetId) {
		QuestionSetEntity questionSet = questionSetReader.getQuestionSet(questionSetId);
		teamRoleValidator.checkHasCreateQuestionSetAuthority(questionSet.getTeamId(), user.id());
		questionSet.endStudyQuestionSet();
		log.info("[학습 문제 셋 종료] questionSetId={}, teamId={}, endedBy={}",
			questionSetId, questionSet.getTeamId(), user.id());
	}

	@Transactional
	public void evaluateAndAutoEnd(final Long questionSetId) {
		QuestionSetEntity questionSet = questionSetReader.getQuestionSet(questionSetId);

		if (!(questionSet.getSolveMode() == QuestionSetSolveMode.STUDY
			&& questionSet.getStatus() == QuestionSetStatus.ONGOING)) {
			return;
		}

		long progressingSessionCount = solvingSessionEntityRepository.countByQuestionSetIdAndSolveModeAndStatus(
			questionSetId, QuestionSetSolveMode.STUDY, SolvingStatus.PROGRESSING);
		if (progressingSessionCount > 0) {
			return;
		}

		long completedSessionCount = solvingSessionEntityRepository.countByQuestionSetIdAndSolveModeAndStatus(
			questionSetId, QuestionSetSolveMode.STUDY, SolvingStatus.COMPLETE);
		long teamMemberCount = teamUserEntityRepository.countByTeamId(questionSet.getTeamId());

		if (completedSessionCount == teamMemberCount) {
			questionSet.endStudyQuestionSet();
			log.info("[학습 문제 셋 자동 종료] questionSetId={}, teamId={}",
				questionSetId, questionSet.getTeamId());
		}
	}
}
