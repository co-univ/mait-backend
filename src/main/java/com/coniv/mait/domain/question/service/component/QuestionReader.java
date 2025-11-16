package com.coniv.mait.domain.question.service.component;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.global.exception.custom.ResourceNotBelongException;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QuestionReader {

	private final QuestionEntityRepository questionEntityRepository;

	public QuestionEntity getQuestion(final Long questionId, final Long questionSetId) {
		QuestionEntity question = questionEntityRepository.findById(questionId)
			.orElseThrow(() -> new EntityNotFoundException("문제를 찾을 수 없습니다."));

		if (!question.getQuestionSet().getId().equals(questionSetId)) {
			throw new ResourceNotBelongException("문제가 해당 문제 세트에 속하지 않습니다.");
		}

		return question;
	}
}
