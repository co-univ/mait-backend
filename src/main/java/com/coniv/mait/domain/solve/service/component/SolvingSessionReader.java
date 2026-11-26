package com.coniv.mait.domain.solve.service.component;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.solve.enums.SolvingStatus;
import com.coniv.mait.domain.solve.repository.SolvingSessionEntityRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SolvingSessionReader {

	private final SolvingSessionEntityRepository solvingSessionEntityRepository;

	public boolean hasCompletedStudySession(final Long userId, final Long questionSetId) {
		return solvingSessionEntityRepository.existsByUserIdAndQuestionSetIdAndSolveModeAndStatus(
			userId, questionSetId, QuestionSetSolveMode.STUDY, SolvingStatus.COMPLETE);
	}
}
