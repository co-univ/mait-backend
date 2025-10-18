package com.coniv.mait.domain.question.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionStatusType;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.component.QuestionFactory;
import com.coniv.mait.domain.question.service.dto.CurrentQuestionDto;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.domain.question.util.LexoRank;
import com.coniv.mait.global.exception.custom.ResourceNotBelongException;

import jakarta.persistence.EntityNotFoundException;

@Service
public class QuestionService {

	private static final QuestionType DEFAULT_QUESTION_TYPE = QuestionType.MULTIPLE;

	private final QuestionEntityRepository questionEntityRepository;

	private final QuestionSetEntityRepository questionSetEntityRepository;

	private final Map<QuestionType, QuestionFactory<?>> questionFactories;

	private final QuestionImageService questionImageService;

	private final MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	@Autowired
	public QuestionService(
		List<QuestionFactory<?>> factories,
		QuestionEntityRepository questionEntityRepository,
		QuestionSetEntityRepository questionSetEntityRepository,
		MultipleChoiceEntityRepository multipleChoiceEntityRepository,
		QuestionImageService questionImageService) {
		questionFactories = factories.stream()
			.collect(Collectors.toUnmodifiableMap(QuestionFactory::getQuestionType, Function.identity()));
		this.questionEntityRepository = questionEntityRepository;
		this.questionSetEntityRepository = questionSetEntityRepository;
		this.questionImageService = questionImageService;
		this.multipleChoiceEntityRepository = multipleChoiceEntityRepository;
	}

	public <T extends QuestionDto> void createQuestion(
		final Long questionSetId,
		final QuestionType type,
		final T questionDto
	) {
		QuestionSetEntity questionSetEntity = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("QuestionSet not found with id: " + questionSetId));

		QuestionFactory<QuestionDto> questionFactory = getQuestionFactory(type);

		questionFactory.save(questionDto, questionSetEntity);
	}

	@Transactional
	public QuestionDto createDefaultQuestion(final Long questionSetId) {
		QuestionSetEntity questionSet = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("QuestionSet not found with id: " + questionSetId));

		final String nextRank = questionEntityRepository.findTopByQuestionSetIdOrderByLexoRankDesc(questionSetId)
			.map(QuestionEntity::getLexoRank)
			.map(LexoRank::nextAfter)
			.orElseGet(LexoRank::middle);

		QuestionEntity defaultQuestion = QuestionEntity.createDefaultQuestion(questionSet, nextRank);
		questionEntityRepository.save(defaultQuestion);

		List<MultipleChoiceEntity> defaultSubEntities = QuestionFactory.createDefaultSubEntities(
			(MultipleQuestionEntity)defaultQuestion);
		multipleChoiceEntityRepository.saveAll(defaultSubEntities);

		return getQuestionFactory(DEFAULT_QUESTION_TYPE).getQuestion(defaultQuestion, true);
	}

	public QuestionDto getQuestion(final Long questionSetId, final Long questionId, final DeliveryMode mode) {
		QuestionEntity question = questionEntityRepository.findById(questionId)
			.orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + questionId));

		if (!question.getQuestionSet().getId().equals(questionSetId)) {
			throw new ResourceNotBelongException("해당 문제 셋에 속한 문제가 아닙니다.");
		}

		boolean answerVisible = mode == null || mode.isAnswerVisible();

		return questionFactories.get(question.getType()).getQuestion(question, answerVisible);
	}

	// Todo: 조회 성능 개선
	public List<QuestionDto> getQuestions(final Long questionSetId) {
		return questionEntityRepository.findAllByQuestionSetId(questionSetId).stream()
			.sorted(Comparator
				.comparing(QuestionEntity::getNumber, Comparator.nullsLast(Long::compareTo))
				.thenComparing(QuestionEntity::getLexoRank))
			.map(question -> getQuestionFactory(question.getType()).getQuestion(question, true))
			.toList();
	}

	@SuppressWarnings("unchecked")
	private <T extends QuestionDto> QuestionFactory<T> getQuestionFactory(
		final QuestionType type
	) {
		QuestionFactory<T> factory = (QuestionFactory<T>)questionFactories.get(type);
		if (factory == null) {
			throw new IllegalArgumentException("지원하지 않는 문제 타입입니다: " + type);
		}
		return factory;
	}

	public CurrentQuestionDto findCurrentQuestion(final Long questionSetId) {
		QuestionSetEntity questionSet = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("QuestionSet not found with id: " + questionSetId));

		Optional<QuestionEntity> mayBeOpenQuestion = questionEntityRepository.findFirstByQuestionSetAndQuestionStatusIn(
			questionSet, List.of(QuestionStatusType.ACCESS_PERMISSION, QuestionStatusType.SOLVE_PERMISSION));

		if (mayBeOpenQuestion.isPresent()) {
			QuestionEntity question = mayBeOpenQuestion.get();
			return CurrentQuestionDto.of(questionSetId, question.getId(), question.getQuestionStatus());
		}

		return CurrentQuestionDto.notOpenQuestion(questionSetId);
	}

	@Transactional
	public QuestionDto updateQuestion(final Long questionSetId, final Long questionId, final QuestionDto questionDto) {
		QuestionEntity question = questionEntityRepository.findById(questionId)
			.orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + questionId));

		if (!question.getQuestionSet().getId().equals(questionSetId)) {
			throw new ResourceNotBelongException("해당 문제 셋에 속한 문제가 아닙니다.");
		}

		QuestionFactory<QuestionDto> questionFactory = getQuestionFactory(questionDto.getType());

		if (question.getType() == questionDto.getType()) {
			question.updateContent(questionDto.getContent());
			question.updateExplanation(questionDto.getExplanation());
			question.updateImageUrl(questionDto.getImageUrl());
			questionImageService.updateImage(question, questionDto.getImageId());

			questionFactory.deleteSubEntities(question);
			questionFactory.createSubEntities(questionDto, question);
			return questionFactory.getQuestion(question, true);
		}

		QuestionFactory<QuestionDto> oldQuestionFactory = getQuestionFactory(question.getType());
		oldQuestionFactory.deleteSubEntities(question);
		questionEntityRepository.delete(question);

		QuestionEntity createdQuestion = questionFactory.save(questionDto, question.getQuestionSet());
		createdQuestion.updateImageUrl(questionDto.getImageUrl());
		questionImageService.updateImage(createdQuestion, questionDto.getImageId());

		return questionFactory.getQuestion(createdQuestion, true);
	}

	@Transactional
	public void deleteQuestion(final Long questionSetId, final Long questionId) {
		QuestionEntity question = questionEntityRepository.findById(questionId)
			.orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + questionId));

		if (!question.getQuestionSet().getId().equals(questionSetId)) {
			throw new ResourceNotBelongException("해당 문제 셋에 속한 문제가 아닙니다.");
		}

		QuestionFactory<?> questionFactory = questionFactories.get(question.getType());
		questionFactory.deleteSubEntities(question);
		questionEntityRepository.deleteById(question.getId());
	}

	@Transactional
	public void changeQuestionOrder(final Long questionSetId, final Long sourceQuestionId, final Long prevQuestionId,
		final Long nextQuestionId) {
		QuestionEntity sourceQuestion = questionEntityRepository.findById(sourceQuestionId)
			.orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + sourceQuestionId));
		if (!sourceQuestion.getQuestionSet().getId().equals(questionSetId)) {
			throw new ResourceNotBelongException("해당 문제 셋에 속한 문제가 아닙니다.");
		}

		if (prevQuestionId == null) {
			QuestionEntity next = questionEntityRepository.findById(nextQuestionId)
				.orElseThrow(() -> new EntityNotFoundException("Next question not found with id: " + nextQuestionId));
			if (!next.getQuestionSet().getId().equals(questionSetId)) {
				throw new ResourceNotBelongException("nextQuestionId가 해당 문제 셋에 속한 문제가 아닙니다.");
			}
			sourceQuestion.updateRank(LexoRank.prevBefore(next.getLexoRank()));
			return;
		}

		if (nextQuestionId == null) {
			QuestionEntity prev = questionEntityRepository.findById(prevQuestionId)
				.orElseThrow(() -> new EntityNotFoundException("Prev question not found with id: " + prevQuestionId));
			if (!prev.getQuestionSet().getId().equals(questionSetId)) {
				throw new ResourceNotBelongException("prevQuestionId가 해당 문제 셋에 속한 문제가 아닙니다.");
			}
			sourceQuestion.updateRank(LexoRank.nextAfter(prev.getLexoRank()));
			return;
		}

		QuestionEntity prev = questionEntityRepository.findById(prevQuestionId)
			.orElseThrow(() -> new EntityNotFoundException("Prev question not found with id: " + prevQuestionId));
		QuestionEntity next = questionEntityRepository.findById(nextQuestionId)
			.orElseThrow(() -> new EntityNotFoundException("Next question not found with id: " + nextQuestionId));

		if (!prev.getQuestionSet().getId().equals(questionSetId)) {
			throw new ResourceNotBelongException("prevQuestionId가 해당 문제 셋에 속한 문제가 아닙니다.");
		}
		if (!next.getQuestionSet().getId().equals(questionSetId)) {
			throw new ResourceNotBelongException("nextQuestionId가 해당 문제 셋에 속한 문제가 아닙니다.");
		}

		String prevRank = prev.getLexoRank();
		String nextRank = next.getLexoRank();

		String newRank = LexoRank.between(prevRank, nextRank);
		sourceQuestion.updateRank(newRank);
	}
}
