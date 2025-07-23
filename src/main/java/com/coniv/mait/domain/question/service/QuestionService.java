package com.coniv.mait.domain.question.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.component.MultipleQuestionFactory;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionService {

	private final QuestionEntityRepository questionEntityRepository;

	private final QuestionSetEntityRepository questionSetEntityRepository;

	private final MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	private final MultipleQuestionFactory multipleQuestionFactory;

	@Transactional
	public void createMultipleQuestion(
		final Long questionSetId,
		final MultipleQuestionDto multipleQuestionsDto
	) {
		QuestionSetEntity questionSetEntity = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("QuestionSet not found with id: " + questionSetId));

		MultipleQuestionEntity multipleQuestion = multipleQuestionFactory.create(multipleQuestionsDto,
			questionSetEntity);

		List<MultipleChoiceEntity> choices = multipleQuestionFactory.createChoices(multipleQuestionsDto.getChoices(),
			multipleQuestion);

		questionEntityRepository.save(multipleQuestion);
		multipleChoiceEntityRepository.saveAll(choices);
	}
}
