package com.coniv.mait.domain.question.service.component;

import java.util.List;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.OrderingOptionEntity;
import com.coniv.mait.domain.question.entity.OrderingQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.service.dto.OrderingQuestionDto;
import com.coniv.mait.domain.question.service.dto.OrderingQuestionOptionDto;
import com.coniv.mait.global.exception.custom.UserParameterException;
import com.coniv.mait.global.util.RandomUtil;

@Component
public class OrderingQuestionFactory {

	private static final int MAX_DISPLAY_DELAY_MILLISECONDS = 5000;

	public OrderingQuestionEntity create(OrderingQuestionDto dto, QuestionSetEntity questionSetEntity) {
		return OrderingQuestionEntity.builder()
			.number(dto.getNumber())
			.questionSet(questionSetEntity)
			.content(dto.getContent())
			.explanation(dto.getExplanation())
			.displayDelayMilliseconds(RandomUtil.getRandomNumber(MAX_DISPLAY_DELAY_MILLISECONDS))
			.build();
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
