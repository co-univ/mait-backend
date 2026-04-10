package com.coniv.mait.domain.question.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.event.QuestionSetDeletedEvent;
import com.coniv.mait.domain.question.exception.QuestionSetStatusException;
import com.coniv.mait.domain.question.exception.code.QuestionSetStatusExceptionCode;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetParticipantRepository;
import com.coniv.mait.domain.question.service.component.QuestionFactory;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.solve.repository.QuestionScorerEntityRepository;
import com.coniv.mait.domain.solve.repository.SolvingSessionEntityRepository;
import com.coniv.mait.domain.solve.repository.StudyAnswerDraftEntityRepository;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.global.event.MaitEventPublisher;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class QuestionSetDeleteService {

	private final QuestionSetEntityRepository questionSetEntityRepository;
	private final QuestionEntityRepository questionEntityRepository;
	private final QuestionSetParticipantRepository questionSetParticipantRepository;
	private final SolvingSessionEntityRepository solvingSessionEntityRepository;
	private final AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;
	private final QuestionScorerEntityRepository questionScorerEntityRepository;
	private final StudyAnswerDraftEntityRepository studyAnswerDraftEntityRepository;
	private final Map<QuestionType, QuestionFactory<?>> questionFactories;
	private final TeamRoleValidator teamRoleValidator;
	private final MaitEventPublisher maitEventPublisher;

	public QuestionSetDeleteService(
		QuestionSetEntityRepository questionSetEntityRepository,
		QuestionEntityRepository questionEntityRepository,
		QuestionSetParticipantRepository questionSetParticipantRepository,
		SolvingSessionEntityRepository solvingSessionEntityRepository,
		AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository,
		QuestionScorerEntityRepository questionScorerEntityRepository,
		StudyAnswerDraftEntityRepository studyAnswerDraftEntityRepository,
		List<QuestionFactory<?>> factories,
		TeamRoleValidator teamRoleValidator,
		MaitEventPublisher maitEventPublisher
	) {
		this.questionSetEntityRepository = questionSetEntityRepository;
		this.questionEntityRepository = questionEntityRepository;
		this.questionSetParticipantRepository = questionSetParticipantRepository;
		this.solvingSessionEntityRepository = solvingSessionEntityRepository;
		this.answerSubmitRecordEntityRepository = answerSubmitRecordEntityRepository;
		this.questionScorerEntityRepository = questionScorerEntityRepository;
		this.studyAnswerDraftEntityRepository = studyAnswerDraftEntityRepository;
		this.questionFactories = factories.stream()
			.collect(Collectors.toUnmodifiableMap(QuestionFactory::getQuestionType, Function.identity()));
		this.teamRoleValidator = teamRoleValidator;
		this.maitEventPublisher = maitEventPublisher;
	}

	@Transactional
	public void deleteQuestionSet(final Long questionSetId, final Long userId) {
		QuestionSetEntity questionSet = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("QuestionSet not found with id: " + questionSetId));

		teamRoleValidator.checkHasCreateQuestionSetAuthority(questionSet.getTeamId(), userId);
		if (questionSet.getStatus() == QuestionSetStatus.ONGOING
			&& questionSet.getSolveMode() == QuestionSetSolveMode.LIVE_TIME) {
			throw new QuestionSetStatusException(QuestionSetStatusExceptionCode.CANNOT_DELETE_ONGOING);
		}

		List<QuestionEntity> questions = questionEntityRepository.findAllByQuestionSetId(questionSetId);
		List<Long> questionIds = questions.stream().map(QuestionEntity::getId).toList();
		List<Long> sessionIds = solvingSessionEntityRepository.findSessionIdsByQuestionSetId(questionSetId);

		List<Long> imageIds = questions.stream()
			.map(QuestionEntity::getImageId)
			.filter(Objects::nonNull)
			.toList();

		if (!sessionIds.isEmpty()) {
			studyAnswerDraftEntityRepository.deleteAllBySolvingSessionIdIn(sessionIds);
		}
		if (!questionIds.isEmpty()) {
			answerSubmitRecordEntityRepository.deleteAllByQuestionIdIn(questionIds);
			questionScorerEntityRepository.deleteAllByQuestionIdIn(questionIds);
		}

		questionSetParticipantRepository.deleteAllByQuestionSet(questionSet);
		solvingSessionEntityRepository.deleteAllByQuestionSetId(questionSetId);

		for (QuestionEntity question : questions) {
			questionFactories.get(question.getType()).deleteSubEntities(question);
		}
		questionEntityRepository.deleteAllByQuestionSetId(questionSetId);

		questionSetEntityRepository.delete(questionSet);

		log.info("[문제셋 삭제] questionSetId={}, teamId={}, questionCount={}, sessionCount={}, deletedBy={}",
			questionSetId, questionSet.getTeamId(), questionIds.size(), sessionIds.size(), userId);

		maitEventPublisher.publishEvent(new QuestionSetDeletedEvent(questionSetId, questionIds, imageIds));
	}

	private void validateDeletableStatus(QuestionSetEntity questionSet) {
		if (questionSet.getStatus() == QuestionSetStatus.ONGOING) {
			throw new QuestionSetStatusException(QuestionSetStatusExceptionCode.CANNOT_DELETE_ONGOING);
		}
	}
}
