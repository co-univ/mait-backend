package com.coniv.mait.domain.question.service.component;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.constant.QuestionConstant;
import com.coniv.mait.domain.question.entity.OrderingOptionEntity;
import com.coniv.mait.domain.question.entity.OrderingQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.OrderingOptionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.service.dto.OrderingQuestionDto;
import com.coniv.mait.domain.question.service.dto.OrderingQuestionOptionDto;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.global.exception.custom.UserParameterException;
import com.coniv.mait.global.util.RandomUtil;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderingQuestionFactory implements QuestionFactory<OrderingQuestionDto> {

	private static final int DEFAULT_OPTION_COUNT = 2;

	private static final String DEFAULT_OPTION_CONTENT_PREFIX = "보기 ";

	private final QuestionEntityRepository questionEntityRepository;

	private final OrderingOptionEntityRepository orderingOptionEntityRepository;

	@Override
	public QuestionType getQuestionType() {
		return QuestionType.ORDERING;
	}

	@Transactional
	@Override
	public QuestionEntity save(OrderingQuestionDto questionDto, QuestionSetEntity questionSetEntity) {
		OrderingQuestionEntity question = create(questionDto, questionSetEntity);
		questionEntityRepository.save(question);

		createSubEntities(questionDto, question);
		return question;
	}

	@Override
	public QuestionDto getQuestion(QuestionEntity question, boolean answerVisible) {
		List<OrderingOptionEntity> options = orderingOptionEntityRepository.findAllByOrderingQuestionId(
			question.getId());
		return OrderingQuestionDto.of((OrderingQuestionEntity)question, options, answerVisible);
	}

	@Override
	public void deleteSubEntities(QuestionEntity question) {
		orderingOptionEntityRepository.deleteAllByOrderingQuestionId(question.getId());
	}

	@Override
	public void createSubEntities(OrderingQuestionDto questionDto, QuestionEntity question) {
		List<OrderingOptionEntity> options = createOrderingQuestionOptions(questionDto.getOptions(),
			(OrderingQuestionEntity)question);
		orderingOptionEntityRepository.saveAll(options);
	}

	@Override
	public OrderingQuestionEntity create(OrderingQuestionDto dto, QuestionSetEntity questionSetEntity) {
		return OrderingQuestionEntity.builder()
			.number(dto.getNumber())
			.questionSet(questionSetEntity)
			.content(dto.getContent())
			.explanation(dto.getExplanation())
			.build();
	}

	@Override
	@Transactional
	public OrderingQuestionEntity createDefaultQuestion(String lexoRank, QuestionSetEntity questionSetEntity) {
		OrderingQuestionEntity orderingQuestion = OrderingQuestionEntity.builder()
			.lexoRank(lexoRank)
			.questionSet(questionSetEntity)
			.build();

		questionEntityRepository.save(orderingQuestion);

		final List<OrderingOptionEntity> defaultOptions = new ArrayList<>();

		for (int i = 1; i <= DEFAULT_OPTION_COUNT; i++) {
			OrderingOptionEntity option = OrderingOptionEntity.builder()
				.content(DEFAULT_OPTION_CONTENT_PREFIX + i)
				.originOrder(i)
				.answerOrder(i)
				.orderingQuestionId(orderingQuestion.getId())
				.build();
			defaultOptions.add(option);
		}

		orderingOptionEntityRepository.saveAll(defaultOptions);

		return orderingQuestion;
	}

	public List<OrderingOptionEntity> createOrderingQuestionOptions(
		List<OrderingQuestionOptionDto> optionDtos,
		OrderingQuestionEntity orderingQuestionEntity
	) {
		checkDuplicateNumber(optionDtos);

		return optionDtos.stream()
			.map(optionDto -> OrderingOptionEntity.builder()
				.content(optionDto.getContent())
				.originOrder(optionDto.getOriginOrder())
				.answerOrder(optionDto.getAnswerOrder())
				.orderingQuestionId(orderingQuestionEntity.getId())
				.build())
			.toList();
	}

	private void checkDuplicateNumber(List<OrderingQuestionOptionDto> optionDtos) {
		long distinctCount = optionDtos.stream()
			.map(OrderingQuestionOptionDto::getAnswerOrder)
			.distinct()
			.count();

		if (distinctCount != optionDtos.size()) {
			throw new UserParameterException("Ordering question options must have unique answer orders.");
		}

		long count = optionDtos.stream()
			.map(OrderingQuestionOptionDto::getOriginOrder)
			.distinct()
			.count();
		if (count != optionDtos.size()) {
			throw new UserParameterException("Ordering question options must have unique origin orders.");
		}
	}
}
