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

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionStatusType;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.component.QuestionFactory;
import com.coniv.mait.domain.question.service.dto.CurrentQuestionDto;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.global.exception.custom.ResourceNotBelongException;

import jakarta.persistence.EntityNotFoundException;

@Service
public class QuestionService {

	private final QuestionEntityRepository questionEntityRepository;

	private final QuestionSetEntityRepository questionSetEntityRepository;

	private final Map<QuestionType, QuestionFactory<?>> questionFactories;

	@Autowired
	public QuestionService(
		List<QuestionFactory<?>> factories,
		QuestionEntityRepository questionEntityRepository,
		QuestionSetEntityRepository questionSetEntityRepository
	) {
		questionFactories = factories.stream()
			.collect(Collectors.toUnmodifiableMap(QuestionFactory::getQuestionType, Function.identity()));
		this.questionEntityRepository = questionEntityRepository;
		this.questionSetEntityRepository = questionSetEntityRepository;
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
			.sorted(Comparator.comparingLong(QuestionEntity::getNumber))
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
	public void updateQuestion(final Long questionId, final Long questionSetId, final QuestionDto questionDto) {
		QuestionEntity question = questionEntityRepository.findById(questionId)
			.orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + questionId));

		if (!question.getQuestionSet().getId().equals(questionSetId)) {
			throw new ResourceNotBelongException("해당 문제 셋에 속한 문제가 아닙니다.");
		}

		question.updateContent(questionDto.getContent());
		question.updateExplanation(questionDto.getExplanation());

		QuestionFactory<QuestionDto> questionFactory = getQuestionFactory(question.getType());

		questionFactory.deleteSubEntities(question);

		questionFactory.createSubEntities(questionDto, question);
	}
}
