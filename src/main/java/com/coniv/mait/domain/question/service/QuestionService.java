package com.coniv.mait.domain.question.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.domain.question.service.component.MultipleQuestionFactory;
import com.coniv.mait.domain.question.service.component.ShortQuestionFactory;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.domain.question.service.dto.ShortQuestionDto;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionService {

	private final QuestionEntityRepository questionEntityRepository;

	private final QuestionSetEntityRepository questionSetEntityRepository;

	private final MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	private final MultipleQuestionFactory multipleQuestionFactory;

	private final ShortQuestionFactory shortQuestionFactory;

	private final ShortAnswerEntityRepository shortAnswerEntityRepository;

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

	@Transactional
	public void createQuestion(
		final Long questionSetId,
		final QuestionType type,
		final QuestionDto questionDto
	) {
		QuestionSetEntity questionSetEntity = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("QuestionSet not found with id: " + questionSetId));

		switch (type) {
			case QuestionType.SHORT -> {
				ShortQuestionDto shortQuestionDto = (ShortQuestionDto)questionDto.toQuestionDto();
				ShortQuestionEntity shortQuestionEntity = shortQuestionFactory.create(shortQuestionDto,
					questionSetEntity);

				questionEntityRepository.save(shortQuestionEntity);
				List<ShortAnswerEntity> shortAnswers = shortQuestionFactory.createShortAnswers(
					shortQuestionDto.getShortAnswers(), shortQuestionEntity);
				shortAnswerEntityRepository.saveAll(shortAnswers);
			}
			default -> throw new IllegalArgumentException("Unsupported question type: " + type);
		}
	}
}
