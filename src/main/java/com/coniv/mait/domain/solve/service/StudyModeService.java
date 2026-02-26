package com.coniv.mait.domain.solve.service;

import org.springframework.stereotype.Service;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;
import com.coniv.mait.domain.solve.repository.SolvingSessionEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.domain.user.service.component.UserReader;
import com.coniv.mait.global.auth.model.MaitUser;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyModeService {

	private final UserReader userReader;
	private final TeamRoleValidator teamRoleValidator;
	private final QuestionSetEntityRepository questionSetEntityRepository;
	private final SolvingSessionEntityRepository solvingSessionEntityRepository;

	public void startStudyMode(final MaitUser maitUser, final Long questionSetId) {
		QuestionSetEntity questionSet = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("존재하지 않는 문제 셋 입니다."));

		UserEntity user = userReader.getById(maitUser.id());

		teamRoleValidator.checkHasSolveQuestionAuthorityInTeam(questionSet.getTeamId(), user.getId());

		SolvingSessionEntity solvingSessionEntity = solvingSessionEntityRepository.findByUserIdAndQuestionSetIdAndMode(
			user.getId(), questionSet.getId(),
			DeliveryMode.STUDY).orElseGet(() -> SolvingSessionEntity.studySession(user, questionSet));

		solvingSessionEntityRepository.save(solvingSessionEntity);
	}
}
