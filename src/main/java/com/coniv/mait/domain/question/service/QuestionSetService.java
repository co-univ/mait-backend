package com.coniv.mait.domain.question.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionSetService {

	private final QuestionSetEntityRepository questionSetEntityRepository;

	@Transactional
	public QuestionSetDto createQuestionSet(final String subject, final QuestionSetCreationType creationType) {
		// Todo: AI 제작인 경우에 분기 필요

		QuestionSetEntity questionSetEntity = QuestionSetEntity.of(subject, creationType);
		questionSetEntityRepository.save(questionSetEntity);

		return QuestionSetDto.builder()
			.id(questionSetEntity.getId())
			.subject(questionSetEntity.getSubject())
			.build();
	}
}
