package com.coniv.mait.domain.question.service.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.service.dto.MultipleChoiceDto;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.global.exception.custom.UserParameterException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MultipleQuestionFactory implements QuestionFactory<MultipleQuestionDto> {

	private static final int DEFAULT_CHOICE_COUNT = 4;

	private final QuestionEntityRepository questionEntityRepository;

	private final MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	@Override
	public QuestionType getQuestionType() {
		return QuestionType.MULTIPLE;
	}

	@Transactional
	@Override
	public QuestionEntity save(MultipleQuestionDto questionDto, QuestionSetEntity questionSetEntity) {
		MultipleQuestionEntity question = create(questionDto, questionSetEntity);
		questionEntityRepository.save(question);

		createSubEntities(questionDto, question);
		return question;
	}

	@Override
	public QuestionDto getQuestion(QuestionEntity question, boolean answerVisible) {
		List<MultipleChoiceEntity> choices = multipleChoiceEntityRepository.findAllByQuestionId(question.getId());
		return MultipleQuestionDto.of((MultipleQuestionEntity)question, choices, answerVisible);
	}

	@Override
	public void deleteSubEntities(QuestionEntity question) {
		multipleChoiceEntityRepository.deleteBulkAllByQuestionId(question.getId());
	}

	@Override
	@Transactional
	public void createSubEntities(MultipleQuestionDto questionDto, QuestionEntity question) {
		MultipleQuestionEntity multipleQuestion = (MultipleQuestionEntity)question;
		List<MultipleChoiceEntity> choices = createChoices(questionDto.getChoices(), multipleQuestion);
		saveChoicesAndUpdateAnswerCounts(choices, List.of(multipleQuestion));
	}

	@Override
	public MultipleQuestionEntity create(MultipleQuestionDto dto, QuestionSetEntity questionSet) {
		return MultipleQuestionEntity.builder()
			.content(dto.getContent())
			.explanation(dto.getExplanation())
			.number(dto.getNumber())
			.questionSet(questionSet)
			.answerCount(calculateAnswerCount(dto.getChoices()))
			.build();
	}

	@Override
	@Transactional
	public MultipleQuestionEntity createDefaultQuestion(String lexoRank, QuestionSetEntity questionSet) {
		MultipleQuestionEntity multipleQuestion = MultipleQuestionEntity.builder()
			.lexoRank(lexoRank)
			.answerCount(0)
			.questionSet(questionSet)
			.build();

		questionEntityRepository.save(multipleQuestion);

		final List<MultipleChoiceEntity> choices = new ArrayList<>();
		for (int number = 1; number <= DEFAULT_CHOICE_COUNT; number++) {
			choices.add(MultipleChoiceEntity.defaultChoice(number, multipleQuestion));
		}
		saveChoicesAndUpdateAnswerCounts(choices, List.of(multipleQuestion));
		return multipleQuestion;
	}

	public List<MultipleChoiceEntity> createChoices(
		List<MultipleChoiceDto> dtos,
		MultipleQuestionEntity question
	) {
		checkChoicesNumber(dtos);
		return dtos.stream()
			.map(dto -> createChoice(dto, question))
			.toList();
	}

	private void checkChoicesNumber(List<MultipleChoiceDto> dtos) {
		long count = dtos.stream()
			.map(MultipleChoiceDto::getNumber)
			.distinct()
			.count();

		if (count != dtos.size()) {
			throw new UserParameterException("중복된 선택지 번호가 존재합니다.");
		}
	}

	private MultipleChoiceEntity createChoice(MultipleChoiceDto dto, MultipleQuestionEntity question) {
		return MultipleChoiceEntity.builder()
			.number(dto.getNumber())
			.content(dto.getContent())
			.question(question)
			.isCorrect(dto.getIsCorrect())
			.build();
	}

	private int calculateAnswerCount(List<MultipleChoiceDto> choices) {
		return (int)choices.stream().filter(MultipleChoiceDto::getIsCorrect).count();
	}

	@Override
	public MultipleQuestionEntity copyQuestion(QuestionEntity source, QuestionSetEntity targetQuestionSet) {
		return MultipleQuestionEntity.builder()
			.content(source.getContent())
			.explanation(source.getExplanation())
			.number(source.getNumber())
			.lexoRank(source.getLexoRank())
			.imageUrl(source.getImageUrl())
			.imageId(source.getImageId())
			.questionSet(targetQuestionSet)
			.answerCount(0)
			.build();
	}

	@Override
	@Transactional
	public void copySubEntities(Map<Long, QuestionEntity> copiedBySourceQuestionId) {
		List<MultipleChoiceEntity> sources = multipleChoiceEntityRepository.findAllByQuestionIdIn(
			List.copyOf(copiedBySourceQuestionId.keySet()));

		List<MultipleChoiceEntity> copies = sources.stream()
			.map(source -> MultipleChoiceEntity.builder()
				.number(source.getNumber())
				.content(source.getContent())
				.isCorrect(source.isCorrect())
				.question((MultipleQuestionEntity)copiedBySourceQuestionId.get(source.getQuestion().getId()))
				.build())
			.toList();

		List<MultipleQuestionEntity> copiedQuestions = copiedBySourceQuestionId.values().stream()
			.map(MultipleQuestionEntity.class::cast)
			.toList();
		saveChoicesAndUpdateAnswerCounts(copies, copiedQuestions);
	}

	private void saveChoicesAndUpdateAnswerCounts(List<MultipleChoiceEntity> choices,
		Collection<MultipleQuestionEntity> questions) {
		multipleChoiceEntityRepository.saveAll(choices);
		Map<MultipleQuestionEntity, Long> answerCounts = choices.stream()
			.filter(MultipleChoiceEntity::isCorrect)
			.collect(Collectors.groupingBy(MultipleChoiceEntity::getQuestion, Collectors.counting()));
		questions.forEach(question -> question.updateAnswerCount(answerCounts.getOrDefault(question, 0L).intValue()));
	}
}
