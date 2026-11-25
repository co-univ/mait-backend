package com.coniv.mait.domain.question.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.question.util.LexoRank;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionRankRebalanceService {

	public static final List<QuestionSetStatus> EDITABLE_STATUSES =
		List.of(QuestionSetStatus.MAKING, QuestionSetStatus.BEFORE);

	private final QuestionSetEntityRepository questionSetEntityRepository;
	private final QuestionReader questionReader;

	@Transactional
	public void rebalance(Long questionSetId) {
		Optional<QuestionSetEntity> editableQuestionSet = questionSetEntityRepository.findByIdForUpdate(questionSetId)
			.filter(questionSet -> EDITABLE_STATUSES.contains(questionSet.getStatus()));
		if (editableQuestionSet.isEmpty()) {
			return;
		}

		List<QuestionEntity> questions = questionReader.getOrderedQuestionsForUpdate(questionSetId);
		if (!LexoRank.needsRebalance(questions.stream().map(QuestionEntity::getLexoRank).toList())) {
			return;
		}
		for (int index = 0; index < questions.size(); index++) {
			questions.get(index).updateRank(LexoRank.evenlySpaced(index, questions.size()));
		}
	}
}
