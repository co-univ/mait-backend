package com.coniv.mait.domain.solve.service.component;

import java.util.List;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;
import com.coniv.mait.domain.solve.entity.StudyAnswerDraftEntity;
import com.coniv.mait.domain.solve.repository.StudyAnswerDraftEntityRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StudyAnswerDraftFactory {

	private final QuestionEntityRepository questionEntityRepository;
	private final StudyAnswerDraftEntityRepository studyAnswerDraftEntityRepository;

	public void createDrafts(final SolvingSessionEntity solvingSession, final Long questionSetId) {
		List<StudyAnswerDraftEntity> drafts = questionEntityRepository
			.findAllByQuestionSetIdOrderByLexoRankAsc(questionSetId).stream()
			.map(question -> StudyAnswerDraftEntity.of(solvingSession, question.getId()))
			.toList();
		studyAnswerDraftEntityRepository.saveAll(drafts);
	}

	public List<StudyAnswerDraftEntity> getDraftsBySolvingSessionId(final Long solvingSessionId) {
		return studyAnswerDraftEntityRepository.findAllByIdSolvingSessionIdOrderByIdQuestionIdAsc(solvingSessionId);
	}
}
