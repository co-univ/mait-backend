package com.coniv.mait.domain.question.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.service.component.QuestionSetReader;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyAutoEndService {

	private final QuestionSetReader questionSetReader;
	private final TeamUserEntityRepository teamUserEntityRepository;

	@Transactional
	public void evaluateAndAutoEnd(final Long questionSetId) {
		QuestionSetEntity questionSet = questionSetReader.getQuestionSet(questionSetId);

		if (shouldSkip(questionSet)) {
			return;
		}

		long uncompletedMemberCount = teamUserEntityRepository.countTeamMembersWithoutCompletedStudySession(
			questionSet.getTeamId(), questionSetId);

		if (uncompletedMemberCount > 0) {
			return;
		}

		questionSet.endStudyQuestionSet();
	}

	private boolean shouldSkip(final QuestionSetEntity questionSet) {
		return questionSet.getSolveMode() != QuestionSetSolveMode.STUDY
			|| questionSet.getStatus() != QuestionSetStatus.ONGOING;
	}
}
