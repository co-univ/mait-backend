package com.coniv.mait.domain.question.service.component;

import java.util.List;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QuestionSetReader {

	private final QuestionSetEntityRepository questionSetEntityRepository;

	public QuestionSetEntity getQuestionSet(final Long questionSetId) {
		return questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException(questionSetId + " : 해당 문제 셋을 찾을 수 없습니다."));
	}

	public List<QuestionSetEntity> getFinishedLiveQuestionSetsInTeam(final Long teamId) {
		return questionSetEntityRepository.findAllByTeamIdAndSolveModeAndStatusIn(teamId,
			QuestionSetSolveMode.LIVE_TIME, List.of(QuestionSetStatus.AFTER, QuestionSetStatus.REVIEW));
	}
}
