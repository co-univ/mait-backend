package com.coniv.mait.domain.question.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.UserStudyStatus;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;
import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;
import com.coniv.mait.domain.solve.enums.SolvingStatus;
import com.coniv.mait.domain.solve.repository.SolvingSessionEntityRepository;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.web.question.dto.StudyQuestionSetGroup;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudyQuestionSetQueryService {

	private final QuestionSetEntityRepository questionSetEntityRepository;
	private final SolvingSessionEntityRepository solvingSessionEntityRepository;
	private final TeamRoleValidator teamRoleValidator;

	@Transactional(readOnly = true)
	public StudyQuestionSetGroup getStudyQuestionSets(final Long teamId, final MaitUser user) {
		final Long userId = user.id();
		teamRoleValidator.checkIsTeamMember(teamId, userId);

		Map<Long, SolvingSessionEntity> sessionByQuestionSetId =
			solvingSessionEntityRepository.findAllByUserIdAndModeAndQuestionSetTeamId(userId, DeliveryMode.STUDY,
					teamId)
				.stream()
				.collect(Collectors.toMap(
					session -> session.getQuestionSet().getId(),
					session -> session,
					(previous, current) -> current
				));

		List<QuestionSetDto> questionSets = questionSetEntityRepository.findAllByTeamId(teamId).stream()
			.filter(questionSet -> questionSet.getDisplayMode() == DeliveryMode.STUDY)
			.sorted(Comparator.comparing(
				QuestionSetEntity::getModifiedAt,
				Comparator.nullsLast(Comparator.naturalOrder())).reversed())
			.map(QuestionSetDto::from)
			.peek(dto -> applyUserStudyStatus(dto, sessionByQuestionSetId.get(dto.getId())))
			.toList();

		return StudyQuestionSetGroup.from(questionSets);
	}

	private void applyUserStudyStatus(final QuestionSetDto questionSet, final SolvingSessionEntity solvingSession) {
		questionSet.setUserStudyStatus(resolveUserStudyStatus(solvingSession));
		if (solvingSession != null) {
			questionSet.setSolvingSessionId(solvingSession.getId());
		}
	}

	private UserStudyStatus resolveUserStudyStatus(final SolvingSessionEntity solvingSession) {
		if (solvingSession == null) {
			return UserStudyStatus.BEFORE;
		}

		SolvingStatus solvingStatus = solvingSession.getStatus();
		return switch (solvingStatus) {
			case PROGRESSING -> UserStudyStatus.ONGOING;
			case COMPLETE -> UserStudyStatus.AFTER;
		};
	}
}
