package com.coniv.mait.domain.question.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.service.component.QuestionSetReader;
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
}
