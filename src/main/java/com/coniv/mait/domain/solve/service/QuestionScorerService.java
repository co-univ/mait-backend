package com.coniv.mait.domain.solve.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.solve.entity.QuestionScorerEntity;
import com.coniv.mait.domain.solve.repository.QuestionScorerEntityRepository;
import com.coniv.mait.domain.solve.service.dto.QuestionScorerDto;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.exception.custom.ResourceNotBelongException;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionScorerService {

	private final UserEntityRepository userEntityRepository;

	private final QuestionEntityRepository questionEntityRepository;

	private final QuestionScorerEntityRepository questionScorerEntityRepository;

	@Transactional(readOnly = true)
	public QuestionScorerDto getScorer(final Long questionSetId, final Long questionId) {
		QuestionEntity question = questionEntityRepository.findById(questionId)
			.orElseThrow(() -> new EntityNotFoundException("문제 ID에 해당하는 문제가 없습니다."));

		if (!question.getQuestionSet().getId().equals(questionSetId)) {
			throw new ResourceNotBelongException("문제 세트 ID와 문제 ID가 일치하지 않습니다.");
		}

		QuestionScorerEntity scorer = questionScorerEntityRepository.findByQuestionId(questionId)
			.orElseThrow(() -> new EntityNotFoundException("해당 문제에 대한 득점자가 없습니다."));

		UserEntity user = userEntityRepository.findById(scorer.getUserId())
			.orElseThrow(() -> new EntityNotFoundException("득점자에 해당하는 사용자가 없습니다."));

		return QuestionScorerDto.of(scorer, user);
	}
}
