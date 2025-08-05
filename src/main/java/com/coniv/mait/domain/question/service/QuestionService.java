package com.coniv.mait.domain.question.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.FillBlankAnswerEntity;
import com.coniv.mait.domain.question.entity.FillBlankQuestionEntity;
import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.OrderingOptionEntity;
import com.coniv.mait.domain.question.entity.OrderingQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.FillBlankAnswerEntityRepository;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.OrderingOptionRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.domain.question.service.component.FillBlankQuestionFactory;
import com.coniv.mait.domain.question.service.component.MultipleQuestionFactory;
import com.coniv.mait.domain.question.service.component.OrderingQuestionFactory;
import com.coniv.mait.domain.question.service.component.ShortQuestionFactory;
import com.coniv.mait.domain.question.service.dto.FillBlankQuestionDto;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;
import com.coniv.mait.domain.question.service.dto.OrderingQuestionDto;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.domain.question.service.dto.ShortQuestionDto;
import com.coniv.mait.global.exception.custom.ResourceNotBelongException;

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

	private final OrderingQuestionFactory orderingQuestionFactory;

	private final OrderingOptionRepository orderingOptionRepository;

	private final ShortAnswerEntityRepository shortAnswerEntityRepository;

	private final FillBlankQuestionFactory fillBlankQuestionFactory;

	private final FillBlankAnswerEntityRepository fillBlankAnswerEntityRepository;

	@Transactional
	public void createQuestion(
		final Long questionSetId,
		final QuestionType type,
		final QuestionDto questionDto
	) {
		QuestionSetEntity questionSetEntity = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("QuestionSet not found with id: " + questionSetId));

		switch (type) {
			case MULTIPLE -> {
				MultipleQuestionDto multipleQuestionsDto = (MultipleQuestionDto)questionDto.toQuestionDto();
				MultipleQuestionEntity multipleQuestion = multipleQuestionFactory.create(multipleQuestionsDto,
					questionSetEntity);

				questionEntityRepository.save(multipleQuestion);

				List<MultipleChoiceEntity> choices = multipleQuestionFactory.createChoices(
					multipleQuestionsDto.getChoices(),
					multipleQuestion);

				multipleChoiceEntityRepository.saveAll(choices);
			}
			case SHORT -> {
				ShortQuestionDto shortQuestionDto = (ShortQuestionDto)questionDto.toQuestionDto();
				ShortQuestionEntity shortQuestionEntity = shortQuestionFactory.create(shortQuestionDto,
					questionSetEntity);

				questionEntityRepository.save(shortQuestionEntity);
				List<ShortAnswerEntity> shortAnswers = shortQuestionFactory.createShortAnswers(
					shortQuestionDto.getShortAnswers(), shortQuestionEntity);
				shortAnswerEntityRepository.saveAll(shortAnswers);
			}
			case ORDERING -> {
				OrderingQuestionDto orderingQuestionDto = (OrderingQuestionDto)questionDto.toQuestionDto();
				OrderingQuestionEntity orderingQuestionEntity = orderingQuestionFactory.create(orderingQuestionDto,
					questionSetEntity);
				questionEntityRepository.save(orderingQuestionEntity);
				List<OrderingOptionEntity> orderingOptions = orderingQuestionFactory
					.createOrderingQuestionOptions(orderingQuestionDto.getOptions(), orderingQuestionEntity);
				orderingOptionRepository.saveAll(orderingOptions);
			}
			case FILL_BLANK -> {
				FillBlankQuestionDto fillBlankQuestionDto = (FillBlankQuestionDto)questionDto.toQuestionDto();
				FillBlankQuestionEntity fillBlankQuestionEntity = fillBlankQuestionFactory.create(fillBlankQuestionDto,
					questionSetEntity);
				questionEntityRepository.save(fillBlankQuestionEntity);

				List<FillBlankAnswerEntity> fillBlankAnswers = fillBlankQuestionFactory.createFillBlankAnswers(
					fillBlankQuestionDto.getFillBlankAnswers(), fillBlankQuestionEntity);
				fillBlankAnswerEntityRepository.saveAll(fillBlankAnswers);
			}
			default -> throw new IllegalArgumentException("Unsupported question type: " + type);
		}
	}

	public QuestionDto getQuestion(final Long questionSetId, final Long questionId) {
		QuestionEntity question = questionEntityRepository.findById(questionId)
			.orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + questionId));

		if (!question.getQuestionSet().getId().equals(questionSetId)) {
			throw new ResourceNotBelongException("해당 문제 셋에 속한 문제가 아닙니다.");
		}

		return mapToQuestionDto(question);
	}

	public List<QuestionDto> getQuestions(final Long questionSetId) {
		return questionEntityRepository.findAllByQuestionSetId(questionSetId).stream()
			.sorted(Comparator.comparingLong(QuestionEntity::getNumber))
			.map(this::mapToQuestionDto)
			.toList();
	}

	private QuestionDto mapToQuestionDto(final QuestionEntity question) {
		switch (question) {
			case MultipleQuestionEntity multipleQuestion -> {
				List<MultipleChoiceEntity> choices = multipleChoiceEntityRepository.findAllByQuestionId(
					multipleQuestion.getId());
				return MultipleQuestionDto.of(multipleQuestion, choices);
			}
			case ShortQuestionEntity shortQuestion -> {
				List<ShortAnswerEntity> shortAnswers = shortAnswerEntityRepository.findAllByShortQuestionId(
					shortQuestion.getId());
				return ShortQuestionDto.of(shortQuestion, shortAnswers);
			}
			case OrderingQuestionEntity orderingQuestion -> {
				List<OrderingOptionEntity> options = orderingOptionRepository.findAllByOrderingQuestionId(
					orderingQuestion.getId());
				return OrderingQuestionDto.of(orderingQuestion, options);
			}
			case FillBlankQuestionEntity fillBlankQuestion -> {
				List<FillBlankAnswerEntity> fillBlankAnswers = fillBlankAnswerEntityRepository
					.findAllByFillBlankQuestionId(fillBlankQuestion.getId());
				return FillBlankQuestionDto.of(fillBlankQuestion, fillBlankAnswers);
			}
			default ->
				throw new IllegalStateException("Unsupported question type: " + question.getClass().getSimpleName());
		}
	}
}
