package com.coniv.mait.domain.question.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.coniv.mait.domain.question.service.component.QuestionSetReader;
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
	private final QuestionSetReader questionSetReader;

	@Autowired
	public QuestionSetDeleteService(
		List<QuestionFactory<?>> factories,
		QuestionSetEntityRepository questionSetEntityRepository,
		QuestionEntityRepository questionEntityRepository,
		QuestionSetParticipantRepository questionSetParticipantRepository,
		SolvingSessionEntityRepository solvingSessionEntityRepository,
		AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository,
		QuestionScorerEntityRepository questionScorerEntityRepository,
		StudyAnswerDraftEntityRepository studyAnswerDraftEntityRepository,
		TeamRoleValidator teamRoleValidator,
		MaitEventPublisher maitEventPublisher,
		QuestionSetReader questionSetReader
	) {
		this.questionFactories = factories.stream()
			.collect(Collectors.toUnmodifiableMap(QuestionFactory::getQuestionType, Function.identity()));
		this.questionSetEntityRepository = questionSetEntityRepository;
		this.questionEntityRepository = questionEntityRepository;
		this.questionSetParticipantRepository = questionSetParticipantRepository;
		this.solvingSessionEntityRepository = solvingSessionEntityRepository;
		this.answerSubmitRecordEntityRepository = answerSubmitRecordEntityRepository;
		this.questionScorerEntityRepository = questionScorerEntityRepository;
		this.studyAnswerDraftEntityRepository = studyAnswerDraftEntityRepository;
		this.teamRoleValidator = teamRoleValidator;
		this.maitEventPublisher = maitEventPublisher;
		this.questionSetReader = questionSetReader;
	}

	@Transactional
	public void deleteQuestionSet(final Long questionSetId, final Long userId) {
		QuestionSetEntity questionSet = questionSetReader.getActiveQuestionSet(questionSetId);

		teamRoleValidator.checkHasCreateQuestionSetAuthority(questionSet.getTeamId(), userId);
		if (questionSet.getStatus() == QuestionSetStatus.ONGOING
			&& questionSet.getSolveMode() == QuestionSetSolveMode.LIVE_TIME) {
			throw new QuestionSetStatusException(QuestionSetStatusExceptionCode.CANNOT_DELETE_ONGOING);
		}

		List<QuestionEntity> questions = questionEntityRepository.findAllByQuestionSetId(questionSetId);
		List<Long> questionIds = questions.stream().map(QuestionEntity::getId).toList();
		List<Long> imageIds = questions.stream()
			.map(QuestionEntity::getImageId)
			.filter(Objects::nonNull)
			.toList();

		if (questionSet.getStatus() != QuestionSetStatus.BEFORE) {
			answerSubmitRecordEntityRepository.deleteAllByQuestionIdIn(questionIds);
		}

		// 참가자 삭제
		if (questionSet.getSolveMode() == QuestionSetSolveMode.LIVE_TIME && questionSet.getStatus().isCompleted()) {
			questionSetParticipantRepository.deleteAllByQuestionSetId(questionSetId);
			questionScorerEntityRepository.deleteAllByQuestionIdIn(questionIds);
		}

		// 풀이 기록 삭제
		if (questionSet.getSolveMode() == QuestionSetSolveMode.STUDY
			&& questionSet.getStatus() != QuestionSetStatus.BEFORE) {
			List<Long> sessionIds = solvingSessionEntityRepository.findSessionIdsByQuestionSetId(questionSetId);
			studyAnswerDraftEntityRepository.deleteAllBySolvingSessionIdIn(sessionIds);
			solvingSessionEntityRepository.deleteAllByQuestionSetId(questionSetId);
		}

		// 문제 삭제
		questions.forEach(question ->
			questionFactories.get(question.getType()).deleteSubEntities(question));
		questionEntityRepository.deleteAllByQuestionSetId(questionSetId);
		questionSetEntityRepository.deleteById(questionSetId);

		maitEventPublisher.publishEvent(QuestionSetDeletedEvent.builder()
			.questionSetId(questionSetId)
			.questionIds(questionIds)
			.imageIds(imageIds)
			.build());

		log.info("[문제셋 삭제] questionSetId={}, teamId={}, questionCount={},  deletedBy={}",
			questionSetId, questionSet.getTeamId(), questionIds.size(), userId);
	}
}
